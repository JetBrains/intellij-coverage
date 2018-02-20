/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package com.intellij.rt.coverage.testDiscovery.instrumentation;

import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

abstract class DefaultConstructorDetectionVisitor extends MethodVisitor {
  private boolean isDefault = true;

  DefaultConstructorDetectionVisitor(int api, MethodVisitor mv) {
    super(api, mv);
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    if (opcode == Opcodes.RETURN) return;
    if (isDefault) {
      isDefault = false;
    }
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    super.visitIntInsn(opcode, operand);
    if (isDefault) {
      isDefault = false;
    }
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
    super.visitMethodInsn(opcode, owner, name, desc, itf);
    if (opcode == Opcodes.INVOKESPECIAL && "<init>".equals(name) && "()V".equals(desc) && !itf) {
      return;
    }
    if (isDefault) {
      isDefault = false;
    }
  }

  @Override
  public void visitVarInsn(int opcode, int var) {
    super.visitVarInsn(opcode, var);
    if (opcode == Opcodes.ALOAD && var == 0) {
      return;
    }
    if (isDefault) {
      isDefault = false;
    }
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    super.visitTypeInsn(opcode, type);
    if (isDefault) {
      isDefault = false;
    }
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String desc) {
    super.visitFieldInsn(opcode, owner, name, desc);
    if (isDefault) {
      isDefault = false;
    }
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    if (isDefault) {
      isDefault = false;
    }
  }

  @Override
  public void visitEnd() {
    super.visitEnd();
    onDecisionDone(isDefault);
  }

  abstract void onDecisionDone(boolean isDefault);
}
