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

import com.intellij.rt.coverage.instrumentation.InstrumentationUtils;
import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.filters.branches.KotlinDefaultArgsBranchFilter;
import com.intellij.rt.coverage.instrumentation.filters.lines.CoverageFilter;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;
import org.jetbrains.coverage.org.objectweb.asm.Type;

/**
 * Skip the branch caused by the current key comparison. The key is passed as an additional last argument.
 */
public class ComposeKeyCheckBranchFilter extends CoverageFilter {
  private int myKeyIndex;
  private State myState = State.INITIAL;
  private int mySavedVarIndex = -1;

  //            SAVE_LAST
  //               ↕
  // INITIAL -> LOAD_LAST → LOAD_CONST → AND →  LOAD_CONST_2
  //               |                      |          |
  //               ↓                      |          |
  //            COMPARE <-----------------|<---------|
  private enum State {
    INITIAL, LOAD_LAST, SAVE_LAST, LOAD_CONST, AND, LOAD_CONST_2, COMPARE
  }

  @Override
  public boolean isApplicable(InstrumentationData context) {
    return ComposeUtils.isComposeMethod(context);
  }

  @Override
  public void initFilter(MethodVisitor methodVisitor, InstrumentationData context) {
    // Default args are inserted directly in the method, no $default method is generated
    KotlinDefaultArgsBranchFilter defaultArgsFilter = new KotlinDefaultArgsBranchFilter();
    defaultArgsFilter.initFilter(methodVisitor, context);
    super.initFilter(defaultArgsFilter, context);
    myKeyIndex = getKeyParameterVarIndex();
  }

  @Override
  public void visitVarInsn(int opcode, int varIndex) {
    super.visitVarInsn(opcode, varIndex);
    if ((varIndex == myKeyIndex || varIndex == mySavedVarIndex) && opcode == Opcodes.ILOAD) {
      myState = State.LOAD_LAST;
    } else if (myState == State.LOAD_LAST && opcode == Opcodes.ISTORE) {
      mySavedVarIndex = varIndex;
      myState = State.SAVE_LAST;
    } else {
      myState = State.INITIAL;
    }
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    boolean isConst = InstrumentationUtils.isIntConstLoading(opcode);
    if (isConst && myState == State.LOAD_LAST) {
      myState = State.LOAD_CONST;
    } else if (isConst && myState == State.AND) {
      myState = State.LOAD_CONST_2;
    } else if (opcode == Opcodes.IAND && myState == State.LOAD_CONST) {
      myState = State.AND;
    } else {
      myState = State.INITIAL;
    }
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    super.visitIntInsn(opcode, operand);
    boolean isConst = InstrumentationUtils.isIntConstLoading(opcode);
    if (isConst && myState == State.LOAD_LAST) {
      myState = State.LOAD_CONST;
    } else if (isConst && myState == State.AND) {
      myState = State.LOAD_CONST_2;
    } else {
      myState = State.INITIAL;
    }
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    if (opcode == Opcodes.IF_ICMPNE && myState == State.LOAD_CONST_2) {
      myContext.removeLastJump();
      myState = State.COMPARE;
    } else if (opcode == Opcodes.IFNE && (myState == State.LOAD_LAST || myState == State.AND)) {
      myContext.removeLastJump();
      myState = State.COMPARE;
    } else {
      myState = State.INITIAL;
    }
  }

  private int getKeyParameterVarIndex() {
    String desc = myContext.getMethodDesc();
    Type[] types = Type.getArgumentTypes(desc);
    int composerIndex = ComposeUtils.getComposerIndex(types);
    if (composerIndex < 0) return -1;
    int index = 0;
    for (int i = 0; i <= composerIndex; i++) {
      index += types[i].getSize();
    }
    if ((Opcodes.ACC_STATIC & myContext.getMethodAccess()) == 0) {
      index++;
    }
    return index;
  }
}
