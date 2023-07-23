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

import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.data.Key;
import com.intellij.rt.coverage.instrumentation.filters.KotlinUtils;
import com.intellij.rt.coverage.instrumentation.filters.lines.CoverageFilter;
import org.jetbrains.coverage.org.objectweb.asm.Label;
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
 * <p>
 * or
 * in Kotlin version greater than 1.4
 * <ol>
 *   <li>GETFIELD</li>
 *   <li>IFNULL</li>
 *   <li>LCD name</li>
 *   <li>INVOKESTATIC kotlin/jvm/internal/Intrinsics.throwUninitializedPropertyAccessException (Ljava/lang/String;)V</li>
 * </ol>
 * For internal classes direct access to a field of the containing class is replaced
 * with access to the containing class (GETFIELD) and then a call to an access method.
 */
public class KotlinLateinitFilter extends CoverageFilter {
  private int myState;

  public boolean isApplicable(InstrumentationData context) {
    return KotlinUtils.isKotlinClass(context);
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    super.visitFieldInsn(opcode, owner, name, descriptor);
    if (myState == 0 && opcode == Opcodes.GETFIELD && myContext.get(Key.CLASS_INTERNAL_NAME).equals(owner)) {
      myState = 1;
      return;
    }
    myState = 0;
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    if ((myState == 1 || myState == 2) && (opcode == Opcodes.IFNONNULL || opcode == Opcodes.IFNULL)) {
      myState = 3;
      return;
    }
    myState = 0;
  }

  @Override
  public void visitLdcInsn(Object value) {
    super.visitLdcInsn(value);
    if (myState == 3 && value instanceof String) {
      myState = 4;
      return;
    }
    myState = 0;
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    if (myState == 4
        && opcode == Opcodes.INVOKESTATIC
        && "kotlin/jvm/internal/Intrinsics".equals(owner)
        && "throwUninitializedPropertyAccessException".equals(name)
        && "(Ljava/lang/String;)V".equals(descriptor)) {
      myContext.removeLastJump();
    } else if (myState == 1
        && opcode == Opcodes.INVOKESTATIC
        && myContext.get(Key.CLASS_INTERNAL_NAME).startsWith(owner)
        && name.startsWith("access$")) {
      myState = 2;
      return;
    }
    myState = 0;
  }
}
