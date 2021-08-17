/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation.filters.visiting;

import com.intellij.rt.coverage.instrumentation.Instrumenter;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Remove } lines from coverage report.
 */
public class ClosingBracesFilter extends MethodVisitingFilter {
  private boolean myHasInstructions;
  private boolean myHasLines;
  private boolean mySeenLineBefore;
  private int myCurrentLine;

  @Override
  public void initFilter(MethodVisitor methodVisitor, Instrumenter context, String desc) {
    super.initFilter(methodVisitor, context, desc);
    myHasInstructions = false;
    myHasLines = false;
    mySeenLineBefore = false;
    myCurrentLine = -1;
  }

  private void removeEmptyLine() {
    if (myHasLines && !myHasInstructions && !mySeenLineBefore) {
      myContext.removeLine(myCurrentLine);
    }
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    removeEmptyLine();
    mySeenLineBefore = myContext.getLineData(line) != null;
    super.visitLineNumber(line, start);
    myHasLines = true;
    myHasInstructions = false;
    myCurrentLine = line;
  }

  @Override
  public void visitEnd() {
    removeEmptyLine();
    super.visitEnd();
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    if (!myHasLines) return;
    if (opcode != Opcodes.NOP && (opcode < Opcodes.IRETURN || opcode > Opcodes.RETURN)) {
      myHasInstructions = true;
    }
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
    if (myHasLines) {
      myHasInstructions = true;
    }
  }

  @Override
  public boolean isApplicable(Instrumenter context, int access, String name,
                              String desc, String signature, String[] exceptions) {
    return true;
  }
}
