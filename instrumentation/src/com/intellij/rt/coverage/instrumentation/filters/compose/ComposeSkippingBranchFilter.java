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

import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.filters.lines.CoverageFilter;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Skip the branch followed by method:
 * invokeinterface androidx/compose/runtime/Composer.getSkipping:()Z
 * IF_NE
 */
public class ComposeSkippingBranchFilter extends CoverageFilter {
  private int myState = 0;

  @Override
  public boolean isApplicable(InstrumentationData context) {
    return ComposeUtils.isComposeMethod(context);
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    if (opcode == Opcodes.INVOKEINTERFACE
        && "androidx/compose/runtime/Composer".equals(owner)
        && "getSkipping".equals(name)
        && "()Z".equals(descriptor)) {
      myState = 1;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    if (opcode == Opcodes.IFNE && myState == 1) {
      myContext.removeLastJump();
    }
    myState = 0;
  }
}
