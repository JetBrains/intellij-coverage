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

package com.intellij.rt.coverage.instrumentation.filters.compose;

import com.intellij.rt.coverage.instrumentation.InstrumentationUtils;
import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.filters.lines.CoverageFilter;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;
import org.jetbrains.coverage.org.objectweb.asm.Type;

/**
 * Skip the branch caused by the current key comparison. The key is passed as an additional last argument.
 * <ol>
 *   <li>ILOAD <last parameter></li>
 *   <li>ICONST/BIPUSH</li>
 *   <li>IAND</li>
 *   <li>ICONST/BIPUSH</li>
 *   <li>if_icmpne</li>
 * </ol>
 * Or alternatively,
 * <ol>
 *   <li>ILOAD <last parameter></li>
 *   <li>IF_NE</li>
 * </ol>
 */
public class ComposeKeyCheckBranchFilter extends CoverageFilter {
  private int myKeyIndex;
  private int myState = 0;

  @Override
  public boolean isApplicable(InstrumentationData context) {
    return ComposeUtils.isComposeMethod(context);
  }

  @Override
  public void initFilter(MethodVisitor methodVisitor, InstrumentationData context) {
    super.initFilter(methodVisitor, context);
    myKeyIndex = getLastParameterIndex();
  }

  @Override
  public void visitVarInsn(int opcode, int varIndex) {
    super.visitVarInsn(opcode, varIndex);
    if (varIndex == myKeyIndex) {
      myState = 1;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    if (InstrumentationUtils.isIntConstLoading(opcode) && (myState == 1 || myState == 3)) {
      myState++;
    } else if (opcode == Opcodes.IAND && myState == 2) {
      myState = 3;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    super.visitIntInsn(opcode, operand);
    if (InstrumentationUtils.isIntConstLoading(opcode) && (myState == 1 || myState == 3)) {
      myState++;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    if (opcode == Opcodes.IF_ICMPNE && myState == 4) {
      myContext.removeLastJump();
    } else if (opcode == Opcodes.IFNE && myState == 1) {
      myContext.removeLastJump();
    } else {
      myState = 0;
    }
  }

  private int getLastParameterIndex() {
    String desc = myContext.getMethodDesc();
    Type[] types = Type.getArgumentTypes(desc);
    int index = 0;
    for (int i = 0; i < types.length - 1; i++) {
      index += types[i].getSize();
    }
    if ((Opcodes.ACC_STATIC & myContext.getMethodAccess()) == 0) {
      index++;
    }
    return index;
  }
}
