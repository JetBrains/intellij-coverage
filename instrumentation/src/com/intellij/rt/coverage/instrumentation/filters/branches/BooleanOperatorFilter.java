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

package com.intellij.rt.coverage.instrumentation.filters.branches;

import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.filters.lines.CoverageFilter;
import org.jetbrains.coverage.org.objectweb.asm.Handle;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Java/Kotlin compilers generate extra instructions to invert boolean value (IFNE) or perfom logical operator (IFEQ):
 * <ol>
 * <li>IFNE/IFEQ LABEL_1</li>
 * <li>ICONST_1</li>
 * <li>GOTO LABEL_2</li>
 * <li>LABEL_1</li>
 * <li>ICONST_0</li>
 * <li>LABEL_2</li>
 * </ol>
 */
public class BooleanOperatorFilter extends CoverageFilter {
  private Label myTrueLabel;
  private Label myFalseLabel;
  private int myState = 0;


  @Override
  public boolean isApplicable(InstrumentationData context) {
    return true;
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    if (opcode == Opcodes.GOTO && myState == 2) {
      myTrueLabel = label;
      myState = 3;
    } else if (opcode == Opcodes.IFEQ || opcode == Opcodes.IFNE) {
      myFalseLabel = label;
      myState = 1;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    if (opcode == Opcodes.ICONST_1 && myState == 1) {
      myState = 2;
    } else if (opcode == Opcodes.ICONST_0 && myState == 4) {
      myState = 5;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitLabel(Label label) {
    super.visitLabel(label);
    if (label == myFalseLabel && myState == 3) {
      myState = 4;
    } else if (label == myTrueLabel && myState == 5) {
      myContext.removeLastJump();
      myState = 0;
    }
  }

  @Override
  public void visitVarInsn(int opcode, int varIndex) {
    super.visitVarInsn(opcode, varIndex);
    myState = 0;
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    super.visitIntInsn(opcode, operand);
    myState = 0;
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    super.visitFieldInsn(opcode, owner, name, descriptor);
    myState = 0;
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    super.visitTypeInsn(opcode, type);
    myState = 0;
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    super.visitLineNumber(line, start);
    myState = 0;
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    myState = 0;
  }

  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    super.visitTableSwitchInsn(min, max, dflt, labels);
    myState = 0;
  }

  @Override
  public void visitIincInsn(int varIndex, int increment) {
    super.visitIincInsn(varIndex, increment);
    myState = 0;
  }

  @Override
  public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
    super.visitMultiANewArrayInsn(descriptor, numDimensions);
    myState = 0;
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    super.visitLookupSwitchInsn(dflt, keys, labels);
    myState = 0;
  }

  @Override
  public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    myState = 0;
  }

  @Override
  public void visitLdcInsn(Object value) {
    super.visitLdcInsn(value);
    myState = 0;
  }
}
