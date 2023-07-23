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
import com.intellij.rt.coverage.instrumentation.filters.KotlinUtils;
import com.intellij.rt.coverage.instrumentation.filters.lines.CoverageFilter;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Kotlin 'as' operator generates a nullability check which adds a IFNONNULL branch.
 * <ol>
 *   <li>IFNONNULL</li>
 *   <li>POP (optional)</li>
 *   <li>NEW java/lang/NullPointerException</li>
 *   <li>DUP</li>
 *   <li>LDC null cannot be cast to non-null type ...</li>
 *   <li>INVOKESPECIAL java/lang/NullPointerException.init (Ljava/lang/String;)V</li>
 *   <li>ATHROW</li>
 * </ol>
 */
public class KotlinUnsafeCastFilter extends CoverageFilter {
  private int myState = 0;

  @Override
  public boolean isApplicable(InstrumentationData context) {
    return KotlinUtils.isKotlinClass(context);
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    if (myState == 1 && opcode == Opcodes.POP) return;
    if (myState == 2 && opcode == Opcodes.DUP) {
      myState++;
    } else if (myState == 5 && opcode == Opcodes.ATHROW) {
      myContext.removeLastJump();
      myState = 0;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    if (myState == 0 && opcode == Opcodes.IFNONNULL) {
      myState++;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    super.visitTypeInsn(opcode, type);
    if (myState == 1 && opcode == Opcodes.NEW && "java/lang/NullPointerException".equals(type)) {
      myState++;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitLdcInsn(Object value) {
    super.visitLdcInsn(value);
    if (myState == 3 && value instanceof String && ((String) value).startsWith("null cannot be cast to non-null type ")) {
      myState++;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    if (myState == 4 && opcode == Opcodes.INVOKESPECIAL
        && "java/lang/NullPointerException".equals(owner)
        && InstrumentationUtils.CONSTRUCTOR.equals(name)
        && "(Ljava/lang/String;)V".equals(descriptor)) {
      myState++;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    super.visitLineNumber(line, start);
    myState = 0;
  }
}
