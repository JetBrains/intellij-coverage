/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation.filters.classFilter;

import com.intellij.rt.coverage.instrumentation.Instrumenter;
import com.intellij.rt.coverage.instrumentation.filters.KotlinUtils;
import org.jetbrains.coverage.org.objectweb.asm.FieldVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

public class KotlinValueClassFilter extends ClassFilter {
  private boolean myEqualsVisited;
  private boolean myToStringVisited;
  private boolean myHashCodeVisited;
  private boolean myConstructorVisited;
  private boolean myBoxingVisited;
  private boolean myUnboxingVisited;
  private int myGetterLine = -1;
  private int myFieldsCount = 0;

  @Override
  public boolean isApplicable(Instrumenter context) {
    return true;
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    final MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
    if (!KotlinUtils.isKotlinClass(myContext)) return mv;
    if ("constructor-impl".equals(name) && (access & Opcodes.ACC_STATIC) != 0) {
      myConstructorVisited = true;
      return mv;
    }
    if ("toString-impl".equals(name) && (access & Opcodes.ACC_STATIC) != 0) {
      myToStringVisited = true;
      return mv;
    }
    if ("equals-impl".equals(name) && (access & Opcodes.ACC_STATIC) != 0) {
      myEqualsVisited = true;
      return mv;
    }
    if ("hashCode-impl".equals(name) && (access & Opcodes.ACC_STATIC) != 0) {
      myHashCodeVisited = true;
      return mv;
    }
    if ("box-impl".equals(name) && (access & Opcodes.ACC_STATIC) != 0 && (access & Opcodes.ACC_FINAL) != 0 && (access & Opcodes.ACC_SYNTHETIC) != 0) {
      myBoxingVisited = true;
      return mv;
    }
    if ("unbox-impl".equals(name) && (access & Opcodes.ACC_FINAL) != 0 && (access & Opcodes.ACC_SYNTHETIC) != 0) {
      myUnboxingVisited = true;
      return mv;
    }
    if (!name.startsWith("get")) return mv;
    return new MethodVisitor(Opcodes.API_VERSION, mv) {
      @Override
      public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        if (myGetterLine == line) return;
        if (myGetterLine == -1) {
          myGetterLine = line;
        } else {
          myGetterLine = -2;
        }
      }
    };
  }

  @Override
  public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
    myFieldsCount++;
    return super.visitField(access, name, descriptor, signature, value);
  }

  @Override
  public void visitEnd() {
    if (myConstructorVisited && myEqualsVisited && myHashCodeVisited
        && myToStringVisited && myBoxingVisited && myUnboxingVisited
        && myFieldsCount == 1
        && myGetterLine >= 0) {
      myContext.removeLine(myGetterLine);
    }
    super.visitEnd();
  }
}
