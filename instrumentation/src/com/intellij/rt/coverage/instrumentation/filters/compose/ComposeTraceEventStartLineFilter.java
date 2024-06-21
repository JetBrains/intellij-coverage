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
import com.intellij.rt.coverage.instrumentation.filters.lines.BaseLineFilter;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Skip lines that consist of only
 * <ol>
 *   <li>invokestatic androidx/compose/runtime/ComposerKt.traceEventStart:(IIILjava/lang/String;)V</li>
 * </ol>
 */
public class ComposeTraceEventStartLineFilter extends BaseLineFilter {
  private int myState = -1;

  @Override
  public boolean isApplicable(InstrumentationData context) {
    return ComposeUtils.isComposeMethod(context);
  }

  @Override
  protected boolean shouldRemoveLine() {
    return myState == 1;
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    super.visitLineNumber(line, start);
    myState = -1;
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    if (myState == -1
        && opcode == Opcodes.INVOKESTATIC
        && "androidx/compose/runtime/ComposerKt".equals(owner)
        && "traceEventStart".equals(name)
        && "(IIILjava/lang/String;)V".equals(descriptor)) {
      myState = 1;
    } else {
      myState = 0;
      setHasInstructions();
    }
  }
}
