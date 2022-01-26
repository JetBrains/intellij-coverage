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

import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.instrumentation.data.BranchDataContainer;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;
import org.jetbrains.coverage.org.objectweb.asm.tree.MethodNode;

public class LineEnumerator extends MethodVisitor implements Opcodes {
  private final AbstractTracingInstrumenter myInstrumenter;
  private final int myAccess;
  private final String myMethodName;
  private final String myDescriptor;
  private final MethodNode myMethodNode;

  protected int myCurrentLine;

  private boolean myHasExecutableLines = false;

  private final MethodVisitor myWriterMethodVisitor;
  protected final BranchDataContainer myBranchData;

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

  protected void onNewJump(Label originalLabel, Label trueLabel, Label falseLabel) {
  }

  protected void onNewSwitch(SwitchLabels original, SwitchLabels replacement) {
  }

  public void visitEnd() {
    super.visitEnd();
    if (myWriterMethodVisitor != SaveHook.EMPTY_METHOD_VISITOR) {
      myMethodNode.accept(myInstrumenter.createTouchCounter(myWriterMethodVisitor, myBranchData, this, myAccess, myMethodName, myDescriptor, getClassName()));
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
        onNewJump(label, trueLabel, falseLabel);

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

  /**
   * Insert new labels before switch in order to let every branch have its own label without fallthrough.
   */
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

  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
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
      onNewSwitch(switchLabels, replacement);
      switchLabels = replacement;
    }
    super.visitLookupSwitchInsn(switchLabels.getDefault(), keys, switchLabels.getLabels());
  }

  public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
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
      onNewSwitch(switchLabels, replacement);
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

  protected static class SwitchLabels {
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
