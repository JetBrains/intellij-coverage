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
import com.intellij.rt.coverage.instrumentation.Instrumenter;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Super calls of the members with default args are prohibited, so an extra `if` is generated.
 *
 * <ol>
 * <li>LINENUMBER</li>
 * <li>ALOAD last argument</li>
 * <li>IFNULL</li>
 * <li>NEW java/lang/UnsupportedOperationException</li>
 * <li>DUP</li>
 * <li>LCD "Super calls with default arguments not supported in this target, function: func"</li>
 * <li>INVOKESPECIAL java/lang/UnsupportedOperationException.init (Ljava/lang/String;)V</li>
 * <li>ATHROW</li>
 * </ol>
 */
public class KotlinOpenMemberWithDefaultArgsFilter extends BranchesFilter {
  private int myState = 0;


  @Override
  public boolean isApplicable(Instrumenter context, int access, String name, String desc, String signature, String[] exceptions) {
    return KotlinDefaultArgsBranchFilter.isApplicable(context, access, name, desc);
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    super.visitLineNumber(line, start);
    if (myState == 0) {
      myState = 1;
      return;
    }
    myState = 0;
  }

  @Override
  public void visitVarInsn(int opcode, int var) {
    super.visitVarInsn(opcode, var);
    if (myState == 1 && opcode == Opcodes.ALOAD) {
      myState = 2;
      return;
    }
    myState = 0;
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    if (myState == 2 && opcode == Opcodes.IFNULL) {
      myState = 3;
      return;
    }
    myState = 0;
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    super.visitTypeInsn(opcode, type);
    if (myState == 3 && opcode == Opcodes.NEW && "java/lang/UnsupportedOperationException".equals(type)) {
      myState = 4;
      return;
    }
    myState = 0;
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    if (myState == 4 && opcode == Opcodes.DUP) {
      myState = 5;
      return;
    }
    if (myState == 7 && opcode == Opcodes.ATHROW) {
      myBranchData.removeLastJump();
    }
    myState = 0;
  }

  @Override
  public void visitLdcInsn(Object value) {
    super.visitLdcInsn(value);
    if (myState == 5 && value instanceof String) {
      final String stringValue = (String) value;
      if (stringValue.startsWith("Super calls with default arguments not supported")) {
        myState = 6;
        return;
      }
    }
    myState = 0;
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    if (myState == 6 && opcode == Opcodes.INVOKESPECIAL
        && "java/lang/UnsupportedOperationException".equals(owner)
        && InstrumentationUtils.CONSTRUCTOR.equals(name)
        && "(Ljava/lang/String;)V".equals(descriptor)) {
      myState = 7;
      return;
    }
    myState = 0;
  }

  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    super.visitTableSwitchInsn(min, max, dflt, labels);
    myState = 0;
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    super.visitLookupSwitchInsn(dflt, keys, labels);
    myState = 0;
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    super.visitIntInsn(opcode, operand);
    myState = 0;
  }

  @Override
  public void visitIincInsn(int var, int increment) {
    super.visitIincInsn(var, increment);
    myState = 0;
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    super.visitFieldInsn(opcode, owner, name, descriptor);
    myState = 0;
  }
}


