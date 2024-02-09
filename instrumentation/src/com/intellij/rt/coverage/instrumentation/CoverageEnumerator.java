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
import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.data.Key;
import com.intellij.rt.coverage.instrumentation.data.SwitchLabels;
import com.intellij.rt.coverage.instrumentation.util.SaveLabelsMethodNode;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;
import org.jetbrains.coverage.org.objectweb.asm.tree.MethodNode;

/**
 * Collect information about coverage and prepare jumps and switches execution for coverage collection.
 * This class uses <code>MethodNode</code> inside, so bytecode is firstly fully analysed and then written to a <code>MethodWriter</code>
 */
public class CoverageEnumerator extends MethodVisitor implements Opcodes {
  private final MethodNode myMethodNode;
  protected final InstrumentationData myData;
  private final String myMethodDesc;
  private final boolean myBranchCoverage;

  protected int myCurrentLine;

  public CoverageEnumerator(InstrumentationData data, boolean branchCoverage) {
    super(Opcodes.API_VERSION, new SaveLabelsMethodNode(
        data.getMethodAccess(), data.getMethodName(), data.getMethodDesc(),
        data.get(Key.METHOD_SIGNATURE), data.get(Key.EXCEPTIONS)));

    myMethodNode = (MethodNode) super.mv;
    myData = data;

    myMethodDesc = data.getProjectContext().getFromPool(myData.getMethodName() + myData.getMethodDesc());
    myBranchCoverage = branchCoverage;
  }

  protected void onNewJump(Label originalLabel, Label trueLabel, Label falseLabel) {
  }

  protected void onNewSwitch(SwitchLabels original, SwitchLabels replacement) {
  }

  public void accept(MethodVisitor visitor) {
    myMethodNode.accept(visitor);
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    myCurrentLine = line;
    myData.createLineData(line, myMethodDesc);
    super.visitLineNumber(line, start);
  }

  @Override
  public void visitJumpInsn(final int opcode, final Label label) {
    if (myData.hasNoLinesInCurrentMethod() || !myBranchCoverage) {
      super.visitJumpInsn(opcode, label);
      return;
    }
    boolean jumpInstrumented = false;
    if (opcode != Opcodes.GOTO && opcode != Opcodes.JSR) {
      LineData lineData = myData.getLineData(myCurrentLine);
      if (lineData != null) {
        Label trueLabel = new Label();
        Label falseLabel = new Label();
        myData.addJump(lineData, trueLabel, falseLabel);
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
    myData.addSwitch(lineData, keys, newDefaultLabel, newLabels);
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

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    if (myData.hasNoLinesInCurrentMethod() || !myBranchCoverage) {
      super.visitLookupSwitchInsn(dflt, keys, labels);
      return;
    }
    final SwitchLabels switchLabels = visitSwitch(dflt, labels, keys);
    super.visitLookupSwitchInsn(switchLabels.getDefault(), keys, switchLabels.getLabels());
  }

  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
    if (myData.hasNoLinesInCurrentMethod() || !myBranchCoverage) {
      super.visitTableSwitchInsn(min, max, dflt, labels);
      return;
    }
    final SwitchLabels switchLabels = visitSwitch(dflt, labels, asLookupKeys(min, max));
    super.visitTableSwitchInsn(min, max, switchLabels.getDefault(), switchLabels.getLabels());
  }

  private SwitchLabels visitSwitch(Label dflt, Label[] labels, int[] keys) {
    SwitchLabels switchLabels = new SwitchLabels(dflt, labels);
    LineData lineData = myData.getLineData(myCurrentLine);
    if (lineData != null) {
      switchLabels = replaceLabels(switchLabels, lineData, keys);
    }
    return switchLabels;
  }

  private static int[] asLookupKeys(int min, int max) {
    int[] keys = new int[max - min + 1];
    // Check that i in [min, max]
    // as there could be an overflow if max == Int.MAX_VALUE
    for (int i = min; min <= i && i <= max; i++) {
      keys[i - min] = i;
    }
    return keys;
  }
}
