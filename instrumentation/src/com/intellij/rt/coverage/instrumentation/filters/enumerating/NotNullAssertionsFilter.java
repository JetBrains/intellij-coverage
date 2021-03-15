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

package com.intellij.rt.coverage.instrumentation.filters.enumerating;

import com.intellij.rt.coverage.instrumentation.Instrumenter;
import com.intellij.rt.coverage.instrumentation.LineEnumerator;
import com.intellij.rt.coverage.util.ClassNameUtil;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Not null assertion should be filtered out.
 * Instructions list of assertion consists of:
 * <ol>
 * <li>DUP</li>
 * <li>IFNONNULL</li>
 * <li>ICONST/BIPUSH</li>
 * <li>INVOKESTATIC className.$$$reportNull$$$0 (I)V</li>
 * </ol>
 * A corresponding jump is filtered out if it's instructions list matches this structure.
 */
public class NotNullAssertionsFilter extends LineEnumeratorFilter {
  private static final byte SEEN_NOTHING = 0;
  private static final byte DUP_SEEN = 1;
  private static final byte IFNONNULL_SEEN = 2;
  private static final byte PARAM_CONST_SEEN = 3;
  private static final byte ASSERTIONS_DISABLED_STATE = 5;

  private byte myState;

  @Override
  public void initFilter(MethodVisitor mv, LineEnumerator context) {
    super.initFilter(mv, context);
    myState = SEEN_NOTHING;
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    if (!myContext.hasExecutableLines()) return;
    if (myState == ASSERTIONS_DISABLED_STATE && opcode == Opcodes.IFNE) {
      myState = SEEN_NOTHING;
      if (myContext.getBranchData().getJump(label) != null) {
        myContext.getBranchData().removeLastJump();
      }
    }
    if (myState == DUP_SEEN && opcode == Opcodes.IFNONNULL) {
      myState = IFNONNULL_SEEN;
    } else {
      myState = SEEN_NOTHING;
    }
  }

  @Override
  public void visitInsn(final int opcode) {
    super.visitInsn(opcode);
    if (!myContext.hasExecutableLines()) return;

    if (opcode == Opcodes.DUP) {
      myState = DUP_SEEN;
    } else if (myState == IFNONNULL_SEEN &&
        (opcode >= Opcodes.ICONST_0 && opcode <= Opcodes.ICONST_5 || opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH)) {
      myState = PARAM_CONST_SEEN;
    } else {
      myState = SEEN_NOTHING;
    }
  }

  @Override
  public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
    super.visitFieldInsn(opcode, owner, name, desc);
    if (!myContext.hasExecutableLines()) return;
    if (opcode == Opcodes.GETSTATIC && name.equals("$assertionsDisabled")) {
      myState = ASSERTIONS_DISABLED_STATE;
    } else {
      myState = SEEN_NOTHING;
    }
  }

  public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
    super.visitMethodInsn(opcode, owner, name, desc, itf);
    if (!myContext.hasExecutableLines()) return;
    if (myState == PARAM_CONST_SEEN &&
        opcode == Opcodes.INVOKESTATIC &&
        name.startsWith("$$$reportNull$$$") &&
        ClassNameUtil.convertToFQName(owner).equals(myContext.getClassName())) {
      myContext.getBranchData().removeLastJump();
    }
    myState = SEEN_NOTHING;
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    super.visitLookupSwitchInsn(dflt, keys, labels);
    setSeenNothingState();
  }

  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    super.visitTableSwitchInsn(min, max, dflt, labels);
    setSeenNothingState();
  }

  @Override
  public void visitIntInsn(final int opcode, final int operand) {
    super.visitIntInsn(opcode, operand);
    setSeenNothingState();
  }

  @Override
  public void visitVarInsn(final int opcode, final int var) {
    super.visitVarInsn(opcode, var);
    setSeenNothingState();
  }

  @Override
  public void visitTypeInsn(final int opcode, final String type) {
    super.visitTypeInsn(opcode, type);
    setSeenNothingState();
  }

  @Override
  public void visitLdcInsn(final Object cst) {
    super.visitLdcInsn(cst);
    setSeenNothingState();
  }

  @Override
  public void visitIincInsn(final int var, final int increment) {
    super.visitIincInsn(var, increment);
    setSeenNothingState();
  }

  @Override
  public void visitMultiANewArrayInsn(final String desc, final int dims) {
    super.visitMultiANewArrayInsn(desc, dims);
    setSeenNothingState();
  }

  private void setSeenNothingState() {
    if (myContext.hasExecutableLines()) {
      myState = SEEN_NOTHING;
    }
  }

  public boolean isApplicable(Instrumenter context, int access, String name, String desc, String signature, String[] exceptions) {
    return true;
  }
}
