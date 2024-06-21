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
import org.jetbrains.coverage.org.objectweb.asm.Handle;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

import java.util.HashSet;
import java.util.Set;

/**
 * Ignore NOP instruction generated on a separate line by the kotlin compiler at the beginning of try-finally block.
 */
public class KotlinTryFinallyLineFilter extends CoverageFilter {
  private final Set<Label> myTryBlockStartLabels = new HashSet<Label>();
  private State myState = State.INITIAL;
  private int myCurrentLine = -1;

  private enum State {
    INITIAL, USER_CODE,
    TRY_START, TRY_START_USER, NOP,
  }

  @Override
  public boolean isApplicable(InstrumentationData context) {
    return KotlinUtils.isKotlinClass(context);
  }

  private void tryRemoveLine() {
    boolean isFinalState = myState == State.NOP;
    if (myCurrentLine != -1 && isFinalState) {
      myContext.removeLine(myCurrentLine);
      myCurrentLine = -1;
    }
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
      } else if (myState == State.USER_CODE) {
        myState = State.TRY_START_USER;
      } else {
        myState = State.INITIAL;
      }
    }
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    if (opcode == Opcodes.NOP && myState == State.TRY_START) {
      myState = State.NOP;
    } else {
      myState = State.USER_CODE;
    }
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    tryRemoveLine();
    boolean lineExists = myContext.getLineData(line) != null;
    super.visitLineNumber(line, start);
    myCurrentLine = line;
    if (lineExists) {
      // do not remove lines that are previously used
      myState = State.USER_CODE;
    } else if (myState == State.USER_CODE) {
      myState = State.INITIAL;
    } else if (myState == State.TRY_START_USER) {
      myState = State.TRY_START;
    }
  }

  @Override
  public void visitEnd() {
    tryRemoveLine();
    super.visitEnd();
  }

  @Override
  public void visitVarInsn(int opcode, int varIndex) {
    super.visitVarInsn(opcode, varIndex);
    myState = State.USER_CODE;
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    myState = State.USER_CODE;
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    super.visitIntInsn(opcode, operand);
    myState = State.USER_CODE;
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    super.visitFieldInsn(opcode, owner, name, descriptor);
    myState = State.USER_CODE;
  }

  @Override
  public void visitIincInsn(int varIndex, int increment) {
    super.visitIincInsn(varIndex, increment);
    myState = State.USER_CODE;
  }

  @Override
  public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    myState = State.USER_CODE;
  }

  @Override
  public void visitLdcInsn(Object value) {
    super.visitLdcInsn(value);
    myState = State.USER_CODE;
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    super.visitLookupSwitchInsn(dflt, keys, labels);
    myState = State.USER_CODE;
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    super.visitTypeInsn(opcode, type);
    myState = State.USER_CODE;
  }

  @Override
  public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
    super.visitMultiANewArrayInsn(descriptor, numDimensions);
    myState = State.USER_CODE;
  }

  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    super.visitTableSwitchInsn(min, max, dflt, labels);
    myState = State.USER_CODE;
  }
}
