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

package com.intellij.rt.coverage.instrumentation.filters.enumerating;

import com.intellij.rt.coverage.data.SwitchData;
import com.intellij.rt.coverage.instrumentation.Instrumenter;
import com.intellij.rt.coverage.instrumentation.kotlin.KotlinUtils;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

public class KotlinWhenMappingExceptionFilter extends LineEnumeratorFilter {

  private Label myCurrentLabel = null;

  @Override
  public boolean isApplicable(Instrumenter context, int access, String name, String desc, String signature, String[] exceptions) {
    return KotlinUtils.isKotlinClass(context);
  }

  @Override
  public void visitLabel(Label label) {
    super.visitLabel(label);
    myCurrentLabel = label;
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    super.visitTypeInsn(opcode, type);
    if (opcode == Opcodes.NEW && type.equals("kotlin/NoWhenBranchMatchedException")) {
      SwitchData switchData = myContext.getDefaultTableSwitchLabels().get(myCurrentLabel);
      if (switchData != null) {
        switchData.touch(-1);
      }
    }
  }
}
