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

package com.intellij.rt.coverage.instrumentation.filters.branches;

import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.filters.lines.CoverageFilter;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Java/Kotlin compilers generate extra instructions to invert boolean value:
 * <ol>
 * <li>IFNE LABEL_1</li>
 * <li>ICONST_1</li>
 * <li>GOTO LABEL_2</li>
 * <li>LABEL_1</li>
 * <li>ICONST_0</li>
 * <li>LABEL_2</li>
 * </ol>
 */
public class BooleanInvertFilter extends CoverageFilter {
  private Label myTrueLabel;
  private Label myFalseLabel;
  private int myState = 0;


  @Override
  public boolean isApplicable(InstrumentationData context) {
    return true;
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    if (opcode == Opcodes.IFNE && myState == 0) {
      myFalseLabel = label;
      myState = 1;
    } else if (opcode == Opcodes.GOTO && myState == 2) {
      myTrueLabel = label;
      myState = 3;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    if (opcode == Opcodes.ICONST_1 && myState == 1) {
      myState = 2;
    } else if (opcode == Opcodes.ICONST_0 && myState == 4) {
      myState = 5;
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitLabel(Label label) {
    super.visitLabel(label);
    if (label == myFalseLabel && myState == 3) {
      myState = 4;
    } else if (label == myTrueLabel && myState == 5) {
      myContext.removeLastJump();
      myState = 0;
    } else {
      myState = 0;
    }
  }
}
