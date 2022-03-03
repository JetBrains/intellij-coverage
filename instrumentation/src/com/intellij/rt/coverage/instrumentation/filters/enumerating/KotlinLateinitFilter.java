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

package com.intellij.rt.coverage.instrumentation.filters.enumerating;

import com.intellij.rt.coverage.instrumentation.Instrumenter;
import com.intellij.rt.coverage.instrumentation.LineEnumerator;
import com.intellij.rt.coverage.instrumentation.kotlin.KotlinUtils;
import com.intellij.rt.coverage.util.ClassNameUtil;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;


/**
 * Lateinit property extra branch should be ignored.
 * <p>
 * Lateinit property access generates such sequence of code inside class:
 * in Kotlin version less or equal than 1.4
 * <ol>
 *   <li>GETFIELD</li>
 *   <li>IFNONNULL</li>
 *   <li>LCD name</li>
 *   <li>INVOKESTATIC kotlin/jvm/internal/Intrinsics.throwUninitializedPropertyAccessException (Ljava/lang/String;)V</li>
 * </ol>
 *
 * or
 * in Kotlin version greater than 1.4
 * <ol>
 *   <li>GETFIELD</li>
 *   <li>IFNULL</li>
 *   <li>LCD name</li>
 *   <li>INVOKESTATIC kotlin/jvm/internal/Intrinsics.throwUninitializedPropertyAccessException (Ljava/lang/String;)V</li>
 * </ol>
 */
public class KotlinLateinitFilter extends LineEnumeratorFilter {
  private int myState;
  private String myInternalClassName;

  public boolean isApplicable(Instrumenter context, int access, String name, String desc, String signature, String[] exceptions) {
    return KotlinUtils.isKotlinClass(context);
  }

  @Override
  public void initFilter(MethodVisitor mv, LineEnumerator context) {
    super.initFilter(mv, context);
    myState = 0;
    myInternalClassName = ClassNameUtil.convertToInternalName(context.getClassName());
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    super.visitFieldInsn(opcode, owner, name, descriptor);
    if (myState == 0 && opcode == Opcodes.GETFIELD && myInternalClassName.equals(owner)) {
      myState = 1;
      return;
    }
    myState = 0;
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    if (myState == 1 && (opcode == Opcodes.IFNONNULL || opcode == Opcodes.IFNULL)) {
      myState = 2;
      return;
    }
    myState = 0;
  }

  @Override
  public void visitLdcInsn(Object value) {
    super.visitLdcInsn(value);
    if (myState == 2 && value instanceof String) {
      myState = 3;
      return;
    }
    myState = 0;
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    if (myState == 3
        && opcode == Opcodes.INVOKESTATIC
        && "kotlin/jvm/internal/Intrinsics".equals(owner)
        && "throwUninitializedPropertyAccessException".equals(name)
        && "(Ljava/lang/String;)V".equals(descriptor)) {
      myContext.getBranchData().removeLastJump();
    }
    myState = 0;
  }
}
