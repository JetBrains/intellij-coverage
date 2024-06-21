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

package com.intellij.rt.coverage.instrumentation.filters.lines;

import org.jetbrains.coverage.org.objectweb.asm.Handle;
import org.jetbrains.coverage.org.objectweb.asm.Label;

/**
 * This is a helper filter to ignore a line.
 * <p>
 * The filter tries to remove a line after the corresponding block is finished.
 * The line is not removed if
 * <ol>
 *   <li>Unexpected instruction is met, expected instructions should not cause {@link #setHasInstructions()} calls</li>
 *   <li>Incorrect state of the filter: {@link #shouldRemoveLine()}</li>
 *   <li>Line was not filtered in the previous blocks</li>
 * </ol>
 *
 * All visitor methods call {@link #setHasInstructions()} by default. When processing the expected instruction,
 * redirect the super call to the parent visitor {@link #mv}. One should call {@link #setHasInstructions()}
 * when the processed instruction is unexpected in the overriding visitor.
 */
public abstract class BaseLineFilter extends CoverageFilter {
  private boolean myHasInstructions;
  private int myCurrentLine = -1;

  // Fields used only for debug purposes.
  // It is convenient to debug by adding conditional breakpoints to `tryRemoveLine` and `setHasInstructions`.
  private static final int DEBUG_LINE = -1;
  private static final Class<?> DEBUG_FILTER_CLASS = BaseLineFilter.class;

  private void tryRemoveLine() {
    if (myCurrentLine != -1 && !myHasInstructions && shouldRemoveLine()) {
      myContext.removeLine(myCurrentLine);
      myCurrentLine = -1;
      onLineRemoved();
    }
  }

  protected abstract boolean shouldRemoveLine();

  protected void onLineRemoved() {
  }

  protected boolean hasInstructions() {
    return myHasInstructions;
  }

  protected void setHasInstructions() {
    myHasInstructions = true;
  }

  protected int getCurrentLine() {
    return myCurrentLine;
  }

  protected boolean wasLineSeenBefore() {
    // do not remove lines that are previously used
    return myContext.getLineData(myCurrentLine) != null;
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    tryRemoveLine();
    myCurrentLine = line;
    myHasInstructions = wasLineSeenBefore();
    super.visitLineNumber(line, start);
  }

  @Override
  public void visitEnd() {
    tryRemoveLine();
    super.visitEnd();
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    setHasInstructions();
  }

  @Override
  public void visitLdcInsn(Object value) {
    super.visitLdcInsn(value);
    setHasInstructions();
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    setHasInstructions();
  }

  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    super.visitTableSwitchInsn(min, max, dflt, labels);
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
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    super.visitLookupSwitchInsn(dflt, keys, labels);
    setHasInstructions();
  }

  @Override
  public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    setHasInstructions();
  }

  @Override
  public void visitIincInsn(int varIndex, int increment) {
    super.visitIincInsn(varIndex, increment);
    setHasInstructions();
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    super.visitFieldInsn(opcode, owner, name, descriptor);
    setHasInstructions();
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    super.visitIntInsn(opcode, operand);
    setHasInstructions();
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    setHasInstructions();
  }

  @Override
  public void visitVarInsn(int opcode, int varIndex) {
    super.visitVarInsn(opcode, varIndex);
    setHasInstructions();
  }
}
