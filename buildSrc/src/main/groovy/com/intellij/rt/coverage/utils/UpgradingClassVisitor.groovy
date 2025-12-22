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

package com.intellij.rt.coverage.utils

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.JSRInlinerAdapter

class UpgradingClassVisitor extends ClassVisitor {
  private int version

  protected UpgradingClassVisitor(ClassVisitor classVisitor) {
    super(Opcodes.ASM9, classVisitor)
  }

  @Override
  void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    this.version = version
    if (version < Opcodes.V11) {
      version = Opcodes.V11
    }
    super.visit(version, access, name, signature, superName, interfaces)
  }

  @Override
  MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    def mv = super.visitMethod(access, name, descriptor, signature, exceptions)
    if (version < Opcodes.V1_7) {
      return new JSRInlinerAdapter(mv, access, name, descriptor, signature, exceptions)
    }
    return mv
  }
}
