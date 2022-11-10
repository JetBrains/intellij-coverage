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

public class BranchesEnumerator extends MethodVisitor implements Opcodes {
  private final AbstractBranchesInstrumenter myInstrumenter;
  private final int myAccess;
  private final String myMethodName;
  private final String myDescriptor;
  private final MethodNode myMethodNode;

  protected int myCurrentLine;

  private boolean myHasExecutableLines = false;

  private final MethodVisitor myWriterMethodVisitor;
  protected final BranchDataContainer myBranchData;

  public BranchesEnumerator(AbstractBranchesInstrumenter instrumenter,
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
    myCurrentLine = line;
    myHasExecutableLines = true;
    final LineData lineData = myInstrumenter.getOrCreateLineData(myCurrentLine, myMethodName, myDescriptor);
    if (lineData != null) myBranchData.addLine(lineData);
    super.visitLineNumber(line, start);
  }

  public void visitJumpInsn(final int opcode, final Label label) {
    if (!myHasExecutableLines) {
      super.visitJumpInsn(opcode, label);
      return;
    }
    boolean jumpInstrumented = false;
    if (opcode != Opcodes.GOTO && opcode != Opcodes.JSR && !InstrumentationUtils.CLASS_INIT.equals(myMethodName)) {
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
  private SwitchLabels replaceLabels(SwitchLabels original, LineData lineData, int[] keys) {
    Label beforeSwitchLabel = new Label();
    Label newDefaultLabel = new Label();
    Label[] newLabels = new Label[original.getLabels().length];
    for (int i = 0; i < original.getLabels().length; i++) {
      newLabels[i] = new Label();
    }

    super.visitJumpInsn(Opcodes.GOTO, beforeSwitchLabel);

    final SwitchLabels replacement = new SwitchLabels(newDefaultLabel, newLabels);
    myBranchData.addSwitch(lineData, lineData.switchesCount(), newDefaultLabel, keys, newLabels);
    onNewSwitch(original, replacement);

    for (int i = 0; i < newLabels.length; i++) {
      super.visitLabel(newLabels[i]);
      super.visitJumpInsn(Opcodes.GOTO, original.getLabels()[i]);
    }

    super.visitLabel(newDefaultLabel);
    super.visitJumpInsn(Opcodes.GOTO, original.getDefault());

    super.visitLabel(beforeSwitchLabel);

    return replacement;
  }

  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    if (!myHasExecutableLines) {
      super.visitLookupSwitchInsn(dflt, keys, labels);
      return;
    }
    final SwitchLabels switchLabels = visitSwitch(dflt, labels, keys);
    super.visitLookupSwitchInsn(switchLabels.getDefault(), keys, switchLabels.getLabels());
  }

  public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
    if (!myHasExecutableLines) {
      super.visitTableSwitchInsn(min, max, dflt, labels);
      return;
    }
    final SwitchLabels switchLabels = visitSwitch(dflt, labels, asLookupKyes(min, max));
    super.visitTableSwitchInsn(min, max, switchLabels.getDefault(), switchLabels.getLabels());
  }

  private SwitchLabels visitSwitch(Label dflt, Label[] labels, int[] keys) {
    SwitchLabels switchLabels = new SwitchLabels(dflt, labels);
    final LineData lineData = myInstrumenter.getLineData(myCurrentLine);
    if (lineData != null) {
      switchLabels = replaceLabels(switchLabels, lineData, keys);
    }
    return switchLabels;
  }

  private static int[] asLookupKyes(int min, int max) {
    int[] keys = new int[max - min + 1];
    // Check that i in [min, max]
    // as there could be an overflow if max == Int.MAX_VALUE
    for (int i = min; min <= i && i <= max; i++) {
      keys[i - min] = i;
    }
    return keys;
  }

  public boolean hasNoLines() {
    return !myHasExecutableLines;
  }

  public String getClassName() {
    return myInstrumenter.getClassName();
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
