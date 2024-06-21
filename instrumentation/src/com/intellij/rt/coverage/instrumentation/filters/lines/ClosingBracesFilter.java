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

package com.intellij.rt.coverage.instrumentation.filters.lines;

import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * This filter ignores lines which consist of return statement only.
 * If a method contains only one line, it cannot be ignored.
 * Also, ignores lines with GOTO statement only (e.g. break in switch).
 */
public class ClosingBracesFilter extends BaseLineFilter {
  private boolean mySeenReturn;
  private boolean mySeenGoto;
  private int myLinesCount = 0;

  @Override
  public boolean isApplicable(InstrumentationData context) {
    return true;
  }

  @Override
  protected boolean shouldRemoveLine() {
    return (mySeenReturn || mySeenGoto) && myLinesCount > 1;
  }

  @Override
  protected void onLineRemoved() {
    myLinesCount--;
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    if (getCurrentLine() != line) myLinesCount++;
    super.visitLineNumber(line, start);
    mySeenReturn = false;
    mySeenGoto = false;
  }

  @Override
  public void visitInsn(int opcode) {
    mv.visitInsn(opcode);
    if (Opcodes.IRETURN <= opcode && opcode <= Opcodes.RETURN) {
      mySeenReturn = true;
      return;
    }
    // ignore code like: POP; LOAD Unit.INSTANCE; ARETURN
    if (opcode == Opcodes.POP) return;
    if (opcode == Opcodes.NOP) return;
    setHasInstructions();
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    mv.visitJumpInsn(opcode, label);
    // ignore single GOTO instructions (e.g. switch break)
    if (opcode == Opcodes.GOTO) {
      mySeenGoto = true;
    } else {
      setHasInstructions();
    }
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    mv.visitFieldInsn(opcode, owner, name, descriptor);
    // ignore return Unit line
    if (opcode == Opcodes.GETSTATIC
        && owner.equals("kotlin/Unit")
        && name.equals("INSTANCE")
        && descriptor.equals("Lkotlin/Unit;")) return;
    setHasInstructions();
  }
}
