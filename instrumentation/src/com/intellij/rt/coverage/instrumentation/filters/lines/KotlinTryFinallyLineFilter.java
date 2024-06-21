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

package com.intellij.rt.coverage.instrumentation.filters.lines;

import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.filters.KotlinUtils;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

import java.util.HashSet;
import java.util.Set;

/**
 * Ignore NOP instruction generated on a separate line by the kotlin compiler at the beginning of try-finally block.
 */
public class KotlinTryFinallyLineFilter extends BaseLineFilter {
  private final Set<Label> myTryBlockStartLabels = new HashSet<Label>();
  private State myState = State.INITIAL;

  private enum State {
    INITIAL,
    TRY_START, NOP,
  }

  @Override
  public boolean isApplicable(InstrumentationData context) {
    return KotlinUtils.isKotlinClass(context);
  }

  @Override
  protected boolean shouldRemoveLine() {
    return myState == State.NOP;
  }

  @Override
  public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
    super.visitTryCatchBlock(start, end, handler, type);
    myTryBlockStartLabels.add(start);
  }

  @Override
  public void visitLabel(Label label) {
    super.visitLabel(label);
    if (myTryBlockStartLabels.contains(label)) {
      if (myState == State.INITIAL) {
        myState = State.TRY_START;
      } else {
        myState = State.INITIAL;
      }
    }
  }

  @Override
  public void visitInsn(int opcode) {
    mv.visitInsn(opcode);
    if (opcode == Opcodes.NOP && myState == State.TRY_START) {
      myState = State.NOP;
    } else {
      setHasInstructions();
    }
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    super.visitLineNumber(line, start);
    if (myState != State.TRY_START) {
      myState = State.INITIAL;
    }
  }
}
