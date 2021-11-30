/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.JumpData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.SwitchData;
import com.intellij.rt.coverage.instrumentation.data.BranchDataContainer;
import com.intellij.rt.coverage.instrumentation.data.Jump;
import com.intellij.rt.coverage.instrumentation.data.Switch;
import org.jetbrains.coverage.org.objectweb.asm.Handle;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;
import org.jetbrains.coverage.org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

public class LineEnumerator extends MethodVisitor implements Opcodes {
  private final AbstractTracingInstrumenter myInstrumenter;
  private final int myAccess;
  private final String myMethodName;
  private final String myDescriptor;
  private final MethodNode myMethodNode;

  private int myCurrentLine;

  private boolean myHasExecutableLines = false;

  private final MethodVisitor myWriterMethodVisitor;
  private final BranchDataContainer myBranchData;

  private final Map<Label, Jump> myOriginalLabelToJump = new HashMap<Label, Jump>();
  private final Map<Label, Switch> myOriginalLabelToSwitch = new HashMap<Label, Switch>();
  private Jump myLastJump;
  private Label myLastLabel;
  private int myInstructionCounter;

  public LineEnumerator(AbstractTracingInstrumenter instrumenter,
                        BranchDataContainer branchData,
                        final MethodVisitor mv,
                        final int access,
                        final String name,
                        final String desc,
                        final String signature,
                        final String[] exceptions) {
    super(Opcodes.API_VERSION, new SaveLabelsMethodNode(access, name, desc, signature, exceptions));

    myMethodNode = (MethodNode) super.mv;
    myInstrumenter = instrumenter;
    myWriterMethodVisitor = mv;
    myAccess = access;
    myMethodName = name;
    myDescriptor = desc;
    myBranchData = branchData;
  }

  private void saveInstructionsToOwner() {
    final LineData lineData = myBranchData.getContext().getLineData(myCurrentLine);
    if (myInstructionCounter > 0 && lineData != null) {
      final Jump jump = myLastJump != null ? myLastJump : (myLastLabel == null ? null : myOriginalLabelToJump.get(myLastLabel));
      final Switch aSwitch = myLastLabel == null ? null : myOriginalLabelToSwitch.get(myLastLabel);

      boolean applied = false;

      if (jump != null) {
        final int jumpId = jump.getId();
        for (int index = 0; index < lineData.jumpsCount(); index++) {
          final JumpData jumpData = lineData.getJumpData(index);
          if (jumpData.getId(true) == jumpId) {
            jumpData.addInstructions(true, myInstructionCounter);
            applied = true;
            break;
          }

          if (jumpData.getId(false) == jumpId) {
            jumpData.addInstructions(false, myInstructionCounter);
            applied = true;
            break;
          }
        }
      } else if (aSwitch != null) {
        final int switchId = aSwitch.getId();
        loop:
        for (int index = 0; index < lineData.switchesCount(); index++) {
          final SwitchData switchData = lineData.getSwitchData(index);
          for (int i = -1; i < switchData.getKeys().length; i++) {
            if (switchData.getId(i) == switchId) {
              switchData.addInstructions(i, myInstructionCounter);
              applied = true;
              break loop;
            }
          }
        }
      }
      if (!applied) {
        lineData.addInstructions(myInstructionCounter);
      }
    }
    myLastLabel = null;
    myLastJump = null;
    myInstructionCounter = 0;
  }


  public void visitEnd() {
    super.visitEnd();
    saveInstructionsToOwner();
    if (myWriterMethodVisitor != SaveHook.EMPTY_METHOD_VISITOR) {
      myMethodNode.accept(myInstrumenter.createTouchCounter(myWriterMethodVisitor, myBranchData, this, myAccess, myMethodName, myDescriptor, getClassName()));
    }
  }

  @Override
  public void visitLabel(Label label) {
    super.visitLabel(label);
    saveInstructionsToOwner();
    if (myOriginalLabelToJump.containsKey(label) || myOriginalLabelToSwitch.containsKey(label)) {
      myLastLabel = label;
    } else {
      myLastLabel = null;
    }
  }

  public void visitLineNumber(int line, Label start) {
    super.visitLineNumber(line, start);
    myCurrentLine = line;
    myHasExecutableLines = true;
    LineData lineData = myInstrumenter.getOrCreateLineData(myCurrentLine, myMethodName, myDescriptor);
    myBranchData.addLine(lineData);
  }

  public void visitJumpInsn(final int opcode, final Label label) {
    saveInstructionsToOwner();
    if (!myHasExecutableLines) {
      super.visitJumpInsn(opcode, label);
      return;
    }
    boolean jumpInstrumented = false;
    if (opcode != Opcodes.GOTO && opcode != Opcodes.JSR && !myMethodName.equals("<clinit>")) {
      final LineData lineData = myInstrumenter.getLineData(myCurrentLine);
      if (lineData != null) {
        int currentJump = lineData.jumpsCount();
        Label trueLabel = new Label();
        Label falseLabel = new Label();
        myBranchData.addJump(lineData, currentJump, trueLabel, falseLabel);
        myLastJump = myBranchData.getJump(falseLabel);
        myOriginalLabelToJump.put(label, myBranchData.getJump(trueLabel));

        jumpInstrumented = true;
        super.visitJumpInsn(opcode, trueLabel);
        super.visitJumpInsn(Opcodes.GOTO, falseLabel);
        super.visitLabel(trueLabel);  // true hit will be inserted here
        super.visitJumpInsn(Opcodes.GOTO, label);
        super.visitLabel(falseLabel); // false hit will be inserted here
      }
    }

    if (!jumpInstrumented) {
      super.visitJumpInsn(opcode, label);
    }
  }

  /** Insert new labels before switch in order to let every branch have its own label without fallthrough. */
  private SwitchLabels replaceLabels(SwitchLabels original) {
    Label beforeSwitchLabel = new Label();
    Label newDefaultLabel = new Label();
    Label[] newLabels = new Label[original.getLabels().length];
    for (int i = 0; i < original.getLabels().length; i++) {
      newLabels[i] = new Label();
    }

    super.visitJumpInsn(Opcodes.GOTO, beforeSwitchLabel);

    for (int i = 0; i < newLabels.length; i++) {
      super.visitLabel(newLabels[i]);
      super.visitJumpInsn(Opcodes.GOTO, original.getLabels()[i]);
    }

    super.visitLabel(newDefaultLabel);
    super.visitJumpInsn(Opcodes.GOTO, original.getDefault());

    super.visitLabel(beforeSwitchLabel);

    return new SwitchLabels(newDefaultLabel, newLabels);
  }

  private void saveSwitchOriginalLabels(SwitchLabels original, SwitchLabels replacement) {
    myOriginalLabelToSwitch.put(original.myDefault, myBranchData.getSwitch(replacement.myDefault));
    for (int i = 0; i < original.myLabels.length; i++) {
      myOriginalLabelToSwitch.put(original.myLabels[i], myBranchData.getSwitch(replacement.myLabels[i]));
    }
  }

  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    saveInstructionsToOwner();
    if (!myHasExecutableLines) {
      super.visitLookupSwitchInsn(dflt, keys, labels);
      return;
    }
    SwitchLabels switchLabels = new SwitchLabels(dflt, labels);
    final LineData lineData = myInstrumenter.getLineData(myCurrentLine);
    if (lineData != null) {
      final SwitchLabels replacement = replaceLabels(switchLabels);
      int switchIndex = lineData.switchesCount();
      myBranchData.addLookupSwitch(lineData, switchIndex, replacement.getDefault(), keys, replacement.getLabels());
      saveSwitchOriginalLabels(switchLabels, replacement);
      switchLabels = replacement;
    }
    super.visitLookupSwitchInsn(switchLabels.getDefault(), keys, switchLabels.getLabels());
  }

  public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
    saveInstructionsToOwner();
    if (!myHasExecutableLines) {
      super.visitTableSwitchInsn(min, max, dflt, labels);
      return;
    }
    SwitchLabels switchLabels = new SwitchLabels(dflt, labels);
    final LineData lineData = myInstrumenter.getLineData(myCurrentLine);
    if (lineData != null) {
      final SwitchLabels replacement = replaceLabels(switchLabels);
      int switchIndex = lineData.switchesCount();
      myBranchData.addTableSwitch(lineData, switchIndex, min, max, replacement.getDefault(), replacement.getLabels());
      saveSwitchOriginalLabels(switchLabels, replacement);
      switchLabels = replacement;
    }
    super.visitTableSwitchInsn(min, max, switchLabels.getDefault(), switchLabels.getLabels());
  }

  public boolean hasExecutableLines() {
    return myHasExecutableLines;
  }

  public String getClassName() {
    return myInstrumenter.getClassName();
  }

  public MethodVisitor getWV() {
    return myWriterMethodVisitor;
  }

  public String getMethodName() {
    return myMethodName;
  }

  public String getDescriptor() {
    return myDescriptor;
  }

  public Instrumenter getInstrumenter() {
    return myInstrumenter;
  }

  public BranchDataContainer getBranchData() {
    return myBranchData;
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    super.visitTypeInsn(opcode, type);
    myInstructionCounter++;
  }

  @Override
  public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
    super.visitMultiANewArrayInsn(descriptor, numDimensions);
    myInstructionCounter++;
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    myInstructionCounter++;
  }

  @Override
  public void visitLdcInsn(Object value) {
    super.visitLdcInsn(value);
    myInstructionCounter++;
  }

  @Override
  public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    myInstructionCounter++;
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    super.visitIntInsn(opcode, operand);
    myInstructionCounter++;
  }

  @Override
  public void visitIincInsn(int var, int increment) {
    super.visitIincInsn(var, increment);
    myInstructionCounter++;
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    super.visitFieldInsn(opcode, owner, name, descriptor);
    myInstructionCounter++;
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    if (Opcodes.IRETURN <= opcode && opcode <= Opcodes.RETURN) {
      return;
    }
    myInstructionCounter++;
  }

  @Override
  public void visitVarInsn(int opcode, int var) {
    super.visitVarInsn(opcode, var);
    myInstructionCounter++;
  }

  private static class SwitchLabels {
    private final Label myDefault;
    private final Label[] myLabels;

    private SwitchLabels(Label dflt, Label[] labels) {
      this.myDefault = dflt;
      this.myLabels = labels;
    }

    public Label getDefault() {
      return myDefault;
    }

    public Label[] getLabels() {
      return myLabels;
    }
  }
}
