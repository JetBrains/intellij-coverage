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
import org.jetbrains.coverage.gnu.trove.TIntHashSet;
import org.jetbrains.coverage.gnu.trove.TIntProcedure;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Remove } lines from coverage report.
 */
public class ClosingBracesFilter extends MethodVisitingFilter {
  private String myName;
  private boolean myHasInstructions;
  private boolean myHasLines;
  private boolean mySeenLineBefore;
  private int myCurrentLine;

  /**
   * Do not ignore return statements in Kotlin inline methods as there is no way to ignore these lines in
   * the inlined code as only NOP instructions stay there.
   */
  private TIntHashSet myLinesToIgnore;
  private boolean myInline;

  @Override
  public void initFilter(MethodVisitor methodVisitor, Instrumenter context, String name, String desc) {
    super.initFilter(methodVisitor, context, name, desc);
    myName = name;
    myHasInstructions = false;
    myHasLines = false;
    mySeenLineBefore = false;
    myCurrentLine = -1;
    myLinesToIgnore = new TIntHashSet();
    myInline = false;
  }

  private void addEmptyLineToRemove() {
    if (myHasLines && !myHasInstructions && !mySeenLineBefore) {
      myLinesToIgnore.add(myCurrentLine);
    }
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    addEmptyLineToRemove();
    mySeenLineBefore = myContext.getLineData(line) != null & !myLinesToIgnore.remove(line);
    super.visitLineNumber(line, start);
    myHasLines = true;
    myHasInstructions = false;
    myCurrentLine = line;
  }

  @Override
  public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
    super.visitLocalVariable(name, descriptor, signature, start, end, index);
    myInline |= KotlinInlineVisitingFilter.isInlineMethod(myName, name);
  }

  @Override
  public void visitEnd() {
    addEmptyLineToRemove();
    if (!myInline) {
      myLinesToIgnore.forEach(new TIntProcedure() {
        public boolean execute(int line) {
          myContext.removeLine(line);
          return true;
        }
      });
    }
    super.visitEnd();
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    if (!myHasLines) return;
    if (opcode < Opcodes.IRETURN || opcode > Opcodes.RETURN) {
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
