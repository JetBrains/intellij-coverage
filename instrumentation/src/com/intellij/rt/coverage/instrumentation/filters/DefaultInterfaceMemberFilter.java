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

import org.jetbrains.coverage.org.objectweb.asm.tree.InsnList;
import org.jetbrains.coverage.org.objectweb.asm.tree.MethodNode;

/**
 * Default interface member should be filtered out.
 * Instructions list of such method consists exactly of:
 * 0. LABEL
 * 1. LINENUMBER
 * 2. ALOAD 0
 * 3. INVOKESTATIC to INTERFACE_NAME$DefaultImpls.INTERFACE_MEMBER
 * 4. RETURN
 * 5. LABEL
 * A method is filtered out is it's instructions list matches this structure.
 */
public class DefaultInterfaceMemberFilter implements MethodFilter {
  public boolean shouldBeCovered(MethodNode methodNode) {
    InsnList instructions = methodNode.instructions;
    return !(instructions.size() == 6
            && FilterUtils.isLabelNode(instructions.get(0))
            && FilterUtils.isLineNumberInstruction(instructions.get(1))
            && FilterUtils.isALoadInstruction(instructions.get(2), 0)
            && FilterUtils.isInvokeStaticInstructionEndsWith(instructions.get(3), "$DefaultImpls", methodNode.name)
            && FilterUtils.isReturnInstruction(instructions.get(4))
            && FilterUtils.isLabelNode(instructions.get(5))

    );
  }
}
