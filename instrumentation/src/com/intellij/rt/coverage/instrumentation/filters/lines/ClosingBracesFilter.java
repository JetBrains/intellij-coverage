/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation.filters.lines;

import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * This filter ignores lines which consist of return statement only.
 * If a method contains only one line, it cannot be ignored.
 * Also, ignores lines with GOTO statement only (e.g. break in switch).
 */
public class ClosingBracesFilter extends CoverageFilter {
  private boolean myHasInstructions;
  private int myCurrentLine = -1;
  private boolean mySeenReturn;
  private boolean mySeenGoto;
  private int myLinesCount = 0;

  private void tryRemoveLine() {
    if (myCurrentLine != -1 && (mySeenReturn || mySeenGoto) && !myHasInstructions && myLinesCount > 1) {
      myContext.removeLine(myCurrentLine);
      myLinesCount--;
      myCurrentLine = -1;
    }
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    tryRemoveLine();
    if (myCurrentLine != line) myLinesCount++;
    myCurrentLine = myContext.getLineData(line) == null ? line : -1;
    myHasInstructions = false;
    mySeenReturn = false;
    mySeenGoto = false;
    super.visitLineNumber(line, start);
  }

  @Override
  public void visitEnd() {
    tryRemoveLine();
    super.visitEnd();
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    if (myCurrentLine == -1) return;
    if (Opcodes.IRETURN <= opcode && opcode <= Opcodes.RETURN) {
      mySeenReturn = true;
      return;
    }
    // ignore code like: POP; LOAD Unit.INSTANCE; ARETURN
    if (opcode == Opcodes.POP) return;
    if (opcode == Opcodes.NOP) return;
    setHasInstructions();
  }

  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    super.visitTableSwitchInsn(min, max, dflt, labels);
    setHasInstructions();
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    super.visitLookupSwitchInsn(dflt, keys, labels);
    setHasInstructions();
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    // ignore single GOTO instructions (e.g. switch break)
    if (opcode == Opcodes.GOTO) {
      mySeenGoto = true;
      return;
    }
    setHasInstructions();
  }

  @Override
  public void visitLdcInsn(Object value) {
    super.visitLdcInsn(value);
    setHasInstructions();
  }

  @Override
  public void visitIincInsn(int var, int increment) {
    super.visitIincInsn(var, increment);
    setHasInstructions();
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    super.visitIntInsn(opcode, operand);
    setHasInstructions();
  }

  @Override
  public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
    super.visitMultiANewArrayInsn(descriptor, numDimensions);
    setHasInstructions();
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    super.visitTypeInsn(opcode, type);
    setHasInstructions();
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    super.visitFieldInsn(opcode, owner, name, descriptor);
    // ignore return Unit line
    if (opcode == Opcodes.GETSTATIC
        && owner.equals("kotlin/Unit")
        && name.equals("INSTANCE")
        && descriptor.equals("Lkotlin/Unit;")) return;
    setHasInstructions();
  }

  @Override
  public void visitVarInsn(int opcode, int var) {
    super.visitVarInsn(opcode, var);
    setHasInstructions();
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    setHasInstructions();
  }

  private void setHasInstructions() {
    myHasInstructions |= !mySeenReturn && myCurrentLine != -1;
  }

  @Override
  public boolean isApplicable(InstrumentationData context) {
    return true;
  }
}
