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

package com.intellij.rt.coverage.instrumentation.filters.branches;

import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.data.Key;
import com.intellij.rt.coverage.instrumentation.filters.lines.CoverageFilter;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Not null assertion should be filtered out.
 * Instruction list of assertion consists of:
 * <ol>
 * <li>IFNONNULL</li>
 * <li>ICONST/BIPUSH</li>
 * <li>INVOKESTATIC className.$$$reportNull$$$0 (I)V</li>
 * </ol>
 */
public class NotNullAssertionsFilter extends CoverageFilter {
  private int myState = 0;

  @Override
  public boolean isApplicable(InstrumentationData context) {
    return true;
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    if (opcode == Opcodes.IFNONNULL) {
      myState = 1;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    if (myState == 1 && Opcodes.ICONST_0 <= opcode && opcode <= Opcodes.ICONST_5) {
      myState++;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitIntInsn(final int opcode, final int operand) {
    super.visitIntInsn(opcode, operand);
    if (myState == 1 && (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH)) {
      myState++;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
    super.visitMethodInsn(opcode, owner, name, desc, itf);
    if (myState == 2 &&
        opcode == Opcodes.INVOKESTATIC &&
        name.startsWith("$$$reportNull$$$") &&
        "(I)V".equals(desc) &&
        myContext.get(Key.CLASS_INTERNAL_NAME).equals(owner)) {
      myContext.removeLastJump();
    }
    myState = 0;
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    super.visitLookupSwitchInsn(dflt, keys, labels);
    myState = 0;
  }

  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    super.visitTableSwitchInsn(min, max, dflt, labels);
    myState = 0;
  }

  @Override
  public void visitVarInsn(final int opcode, final int var) {
    super.visitVarInsn(opcode, var);
    myState = 0;
  }

  @Override
  public void visitTypeInsn(final int opcode, final String type) {
    super.visitTypeInsn(opcode, type);
    myState = 0;
  }

  @Override
  public void visitLdcInsn(final Object cst) {
    super.visitLdcInsn(cst);
    myState = 0;
  }

  @Override
  public void visitIincInsn(final int var, final int increment) {
    super.visitIincInsn(var, increment);
    myState = 0;
  }

  @Override
  public void visitMultiANewArrayInsn(final String desc, final int dims) {
    super.visitMultiANewArrayInsn(desc, dims);
    myState = 0;
  }
}
