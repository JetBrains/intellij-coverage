/*
 * Copyright 2000-2024 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation.util;

import org.jetbrains.coverage.org.objectweb.asm.Handle;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;

/**
 * Kotlin compiler may insert jumps in the middle of a line.
 * In this case, the coverage may report uncovered lines, as the hits[line] markers are inserted to the line label.
 * To fix this issue, this visitor finds jumps in the middle of the line and inserts fictive line labels before them.
 * The lines are only inserted if the jump was from a different line.
 */
public class InsertLineAfterJumpVisitor extends MethodVisitor {
  private final Map<Label, Integer> myLabelsWithoutLines = new HashMap<Label, Integer>();
  private Label myCurrentLabel;
  private int myCurrentLine = -1;

  public InsertLineAfterJumpVisitor(MethodVisitor methodVisitor) {
    super(Opcodes.API_VERSION, methodVisitor);
  }

  @Override
  public void visitLabel(Label label) {
    myCurrentLabel = label;
    super.visitLabel(label);
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    myCurrentLine = line;
    myLabelsWithoutLines.remove(start);
    super.visitLineNumber(line, start);
  }

  private void checkHasLineIfAfterJump() {
    if (myCurrentLine == -1) return;
    if (myCurrentLabel == null) return;
    Integer jumpedFromLine = myLabelsWithoutLines.remove(myCurrentLabel);
    if (jumpedFromLine != null && jumpedFromLine != myCurrentLine) {
      visitLineNumber(myCurrentLine, myCurrentLabel);
    }
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    checkHasLineIfAfterJump();
    super.visitJumpInsn(opcode, label);
    if (myCurrentLabel == null) return;
    if (myCurrentLine == -1) return;
    myLabelsWithoutLines.put(label, myCurrentLine);
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    checkHasLineIfAfterJump();
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
  }

  @Override
  public void visitLdcInsn(Object value) {
    checkHasLineIfAfterJump();
    super.visitLdcInsn(value);
  }

  @Override
  public void visitInsn(int opcode) {
    checkHasLineIfAfterJump();
    super.visitInsn(opcode);
  }

  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    checkHasLineIfAfterJump();
    super.visitTableSwitchInsn(min, max, dflt, labels);
  }

  @Override
  public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
    checkHasLineIfAfterJump();
    super.visitMultiANewArrayInsn(descriptor, numDimensions);
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    checkHasLineIfAfterJump();
    super.visitTypeInsn(opcode, type);
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    checkHasLineIfAfterJump();
    super.visitLookupSwitchInsn(dflt, keys, labels);
  }

  @Override
  public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
    checkHasLineIfAfterJump();
    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
  }

  @Override
  public void visitIincInsn(int varIndex, int increment) {
    checkHasLineIfAfterJump();
    super.visitIincInsn(varIndex, increment);
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    checkHasLineIfAfterJump();
    super.visitFieldInsn(opcode, owner, name, descriptor);
  }

  @Override
  public void visitVarInsn(int opcode, int varIndex) {
    checkHasLineIfAfterJump();
    super.visitVarInsn(opcode, varIndex);
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    checkHasLineIfAfterJump();
    super.visitIntInsn(opcode, operand);
  }
}
