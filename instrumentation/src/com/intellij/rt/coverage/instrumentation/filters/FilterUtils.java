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

package com.intellij.rt.coverage.instrumentation.filters;

import org.jetbrains.coverage.org.objectweb.asm.Opcodes;
import org.jetbrains.coverage.org.objectweb.asm.tree.*;

class FilterUtils {
  static boolean isLineNumberInstruction(AbstractInsnNode instruction) {
    return instruction instanceof LineNumberNode;
  }

  static boolean isALoadInstruction(AbstractInsnNode instruction) {
    return instruction.getOpcode() == Opcodes.ALOAD;
  }

  static boolean isALoadInstruction(AbstractInsnNode instruction, int variableIndex) {
    if (!isALoadInstruction(instruction)) return false;
    VarInsnNode varInstruction = (VarInsnNode) instruction;
    return varInstruction.var == variableIndex;
  }

  static boolean isReturnInstruction(AbstractInsnNode instruction) {
    return instruction.getOpcode() == Opcodes.RETURN;
  }

  public static boolean isInvokeStaticInstructionEndsWith(AbstractInsnNode instruction, String ownerPostfix, String methodName) {
    if (instruction.getOpcode() != Opcodes.INVOKESTATIC) return false;
    MethodInsnNode methodInstruction = (MethodInsnNode) instruction;
    return methodInstruction.owner.endsWith(ownerPostfix)
        && methodInstruction.name.equals(methodName);
  }

  public static boolean isLabelNode(AbstractInsnNode instruction) {
    return instruction instanceof LabelNode;
  }
}
