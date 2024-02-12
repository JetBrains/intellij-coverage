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
import com.intellij.rt.coverage.instrumentation.data.Key;
import com.intellij.rt.coverage.instrumentation.filters.KotlinUtils;
import com.intellij.rt.coverage.instrumentation.filters.lines.CoverageFilter;
import org.jetbrains.coverage.org.objectweb.asm.Handle;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Assert statement creates a check whether assertions are enabled:
 * <ol>
 *   <li>GETSTATIC THIS_CLASS.$assertionsDisabled : Z</li>
 *   <li>IFNE</li>
 * </ol>
 */
public class AssertFilter extends CoverageFilter {
  private int myState = 0;

  @Override
  public boolean isApplicable(InstrumentationData context) {
    return !KotlinUtils.isKotlinClass(context);
  }

  @Override
  public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
    super.visitFieldInsn(opcode, owner, name, desc);
    if (opcode == Opcodes.GETSTATIC
        && "Z".equals(desc)
        && name.equals("$assertionsDisabled")
        && owner.equals(myContext.get(Key.CLASS_INTERNAL_NAME))) {
      myState = 1;
    } else {
      myState = 0;
    }
  }


  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    if (myState == 1 && opcode == Opcodes.IFNE) {
      myContext.removeLastJump();
    }
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
  public void visitIincInsn(int varIndex, int increment) {
    super.visitIincInsn(varIndex, increment);
    myState = 0;
  }

  @Override
  public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    myState = 0;
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    super.visitIntInsn(opcode, operand);
    myState = 0;
  }

  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    super.visitTableSwitchInsn(min, max, dflt, labels);
    myState = 0;
  }

  @Override
  public void visitLdcInsn(Object value) {
    super.visitLdcInsn(value);
    myState = 0;
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    super.visitLookupSwitchInsn(dflt, keys, labels);
    myState = 0;
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    myState = 0;
  }

  @Override
  public void visitVarInsn(int opcode, int varIndex) {
    super.visitVarInsn(opcode, varIndex);
    myState = 0;
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    myState = 0;
  }
}
