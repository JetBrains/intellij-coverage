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

package com.intellij.rt.coverage.instrumentation.filters.branches;

import com.intellij.rt.coverage.instrumentation.InstrumentationUtils;
import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.data.Key;
import com.intellij.rt.coverage.instrumentation.data.SwitchLabels;
import com.intellij.rt.coverage.instrumentation.filters.KotlinUtils;
import com.intellij.rt.coverage.instrumentation.filters.lines.CoverageFilter;
import org.jetbrains.coverage.org.objectweb.asm.Handle;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * In <code>switch</code> by String, Java compiler generates extra branches.
 * Firstly, check for null string if applicable:
 * <ol>
 *   <li>ICONST_M1</li>
 *   <li>ISTORE slot</li>
 *   <li>ALOAD</li>
 *   <li>INVOKEVIRTUAL java/lang/String.hashCode ()I</li>
 *   <li>LOOKUPSWITCH (default: label)</li>
 * </ol>
 * <p>
 * And then in each branch of the switch there is a check for equality:
 *
 * <ol>
 *  <li>ALOAD</li>
 *  <li>LDC String</li>
 *  <li>INVOKEVIRTUAL java/lang/String.equals (Ljava/lang/Object;)Z</li>
 *  <li>IFEQ label</li>
 *  <li>ICONST 0 / 1 / ... / BIPUSH</li>
 *  <li>ISTORE slot</li>
 *  <li>GOTO label (absent for the last branch)</li>
 * </ol>
 * <p>
 * And then another switch by slot:
 * <ol>
 *   <li>LABEL label</li>
 *   <li>ILOAD slot</li>
 *   <li>LOOKUPSWITCH</li>
 * </ol>
 * <p>
 * All branches except for the last switch should be ignored.
 */
public class JavaStringSwitchFilter extends CoverageFilter {
  private int myState = 0;
  private int mySlot = -1;
  private SwitchLabels mySwitch;


  @Override
  public boolean isApplicable(InstrumentationData context) {
    int version = InstrumentationUtils.getBytecodeVersion(context.get(Key.CLASS_READER));
    return version >= Opcodes.V1_7 && !KotlinUtils.isKotlinClass(context);
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    if (opcode == Opcodes.ICONST_M1) {
      myState = 1;
    } else if (myState == 9 && Opcodes.ICONST_0 <= opcode && opcode <= Opcodes.ICONST_5) {
      myState++;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    super.visitIntInsn(opcode, operand);
    if (myState == 9 && opcode == Opcodes.BIPUSH && operand >= 0) {
      myState++;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitVarInsn(int opcode, int varIndex) {
    super.visitVarInsn(opcode, varIndex);
    if (myState == 1 && opcode == Opcodes.ISTORE) {
      myState++;
      mySlot = varIndex;
    } else if (myState == 10 && opcode == Opcodes.ISTORE && varIndex == mySlot) {
      myState++;
    } else if ((myState == 2 || myState == 5) && opcode == Opcodes.ALOAD) {
      myState++;
    } else if (myState == 12 && opcode == Opcodes.ILOAD && varIndex == mySlot) {
      myState++;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    if (myState == 3
        && opcode == Opcodes.INVOKEVIRTUAL
        && "java/lang/String".equals(owner)
        && "hashCode".equals(name)
        && "()I".equals(descriptor)) {
      myState++;
    } else if (myState == 7 && opcode == Opcodes.INVOKEVIRTUAL
        && "java/lang/String".equals(owner)
        && "equals".equals(name)
        && "(Ljava/lang/Object;)Z".equals(descriptor)) {
      myState++;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    visitSwitch(dflt, labels);
    super.visitTableSwitchInsn(min, max, dflt, labels);
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    visitSwitch(dflt, labels);
    super.visitLookupSwitchInsn(dflt, keys, labels);
  }

  private void visitSwitch(Label dflt, Label[] labels) {
    if (myState == 4) {
      myState++;
      mySwitch = new SwitchLabels(dflt, labels);
    } else if (myState == 13 && mySwitch != null) {
      myState = 0;
      myContext.removeLastSwitch();
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitLdcInsn(Object value) {
    super.visitLdcInsn(value);
    if (myState == 6 && value instanceof String) {
      myState++;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    if (myState == 8 && opcode == Opcodes.IFEQ && mySwitch != null) {
      // this could be a jump to the default label or to the next branch with a string with equal hashCode
      myState++;
      myContext.removeLastJump();
    } else if (myState == 11 && opcode == Opcodes.GOTO && mySwitch != null && label == mySwitch.getDefault()) {
      myState = 5; // switch to the next branch
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitLabel(Label label) {
    super.visitLabel(label);
    if (myState == 11 && mySwitch != null && label == mySwitch.getDefault()) {
      myState++;
    }
  }

  @Override
  public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    myState = 0;
  }

  @Override
  public void visitIincInsn(int varIndex, int increment) {
    super.visitIincInsn(varIndex, increment);
    myState = 0;
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    super.visitTypeInsn(opcode, type);
    myState = 0;
  }

  @Override
  public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
    super.visitMultiANewArrayInsn(descriptor, numDimensions);
    myState = 0;
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    super.visitFieldInsn(opcode, owner, name, descriptor);
    myState = 0;
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    super.visitLineNumber(line, start);
    myState = 0;
  }
}
