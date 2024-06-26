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
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Ignore lines generated by compiler for try-with-resources block.
 *
 * @see TryWithResourcesLineFilter.State
 */
public class TryWithResourcesLineFilter extends BaseLineFilter {
  private State myState = State.INITIAL;
  private int myJumpsToRemove = 0;
  private int myExceptionVarIndex = -1;

  // INITIAL → STORE_INITIAL_EXCEPTION/2 <-----|
  //    ↘         ↓                            |
  // ---->LOAD_RESOURCE ↔ CHECK_RESOURCE_NULL  |
  // |         ↕                               |
  // |      CALL_CLOSE---------------|         |
  // |         ↓                     |         |
  // |        GOTO                   |         |
  // |         ↓                     |         |
  // |   STORE_ADDITIONAL_EXCEPTION  |         |
  // |         ↓                     |         |
  // |   LOAD_INITIAL_EXCEPTION      |         |
  // |         ↓                     |         |
  // |   LOAD_ADDITIONAL_EXCEPTION   |         |
  // |         ↓                     |         |
  // |     CALL_ADD_SUPPRESSED       |         |
  // |     ↙          ↓             ↙          |
  // GOTO_2  LOAD_INITIAL_EXCEPTION_2          |
  //                  ↓                        |
  //                 THROW --------------------|
  private enum State {
    INITIAL,
    STORE_INITIAL_EXCEPTION,
    STORE_INITIAL_EXCEPTION_2, // final
    LOAD_RESOURCE,
    CHECK_RESOURCE_NULL,
    CALL_CLOSE, // final
    GOTO, // final
    STORE_ADDITIONAL_EXCEPTION,
    LOAD_INITIAL_EXCEPTION,
    LOAD_ADDITIONAL_EXCEPTION,
    CALL_ADD_SUPPRESSED,
    GOTO_2, // java 8
    LOAD_INITIAL_EXCEPTION_2,
    THROW, // final
  }

  @Override
  public boolean isApplicable(InstrumentationData context) {
    return true;
  }

  @Override
  protected boolean shouldRemoveLine() {
    return myState == State.GOTO || myState == State.THROW || myState == State.CALL_CLOSE
        || myState == State.STORE_INITIAL_EXCEPTION_2;
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    super.visitLineNumber(line, start);
    myState = State.INITIAL;
    myJumpsToRemove = 0;
    myExceptionVarIndex = -1;
  }

  @Override
  public void visitVarInsn(int opcode, int var) {
    mv.visitVarInsn(opcode, var);
    if (opcode == Opcodes.ASTORE) {
      if (myState == State.INITIAL) {
        myState = State.STORE_INITIAL_EXCEPTION;
        myExceptionVarIndex = var;
      } else if (myState == State.THROW) {
        myState = State.STORE_INITIAL_EXCEPTION_2;
        myExceptionVarIndex = var;
      } else if (myState == State.GOTO) {
        myState = State.STORE_ADDITIONAL_EXCEPTION;
      } else {
        setHasInstructions();
        myState = State.INITIAL;
      }
    } else if (opcode == Opcodes.ALOAD) {
      if (myExceptionVarIndex == var && myState == State.CALL_CLOSE) {
        myState = State.LOAD_INITIAL_EXCEPTION_2;
      } else if (myState == State.INITIAL
          || myState == State.STORE_INITIAL_EXCEPTION
          || myState == State.STORE_INITIAL_EXCEPTION_2
          || myState == State.CHECK_RESOURCE_NULL
          || myState == State.GOTO_2
          || myState == State.CALL_CLOSE) {
        myState = State.LOAD_RESOURCE;
      } else if (myState == State.STORE_ADDITIONAL_EXCEPTION) {
        myState = State.LOAD_INITIAL_EXCEPTION;
      } else if (myState == State.LOAD_INITIAL_EXCEPTION) {
        myState = State.LOAD_ADDITIONAL_EXCEPTION;
      } else if (myState == State.CALL_ADD_SUPPRESSED) {
        myState = State.LOAD_INITIAL_EXCEPTION_2;
      } else {
        setHasInstructions();
        myState = State.INITIAL;
      }
    } else {
      setHasInstructions();
      myState = State.INITIAL;
    }
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    mv.visitJumpInsn(opcode, label);
    if (myState == State.LOAD_RESOURCE && opcode == Opcodes.IFNULL) {
      myState = State.CHECK_RESOURCE_NULL;
      myJumpsToRemove++;
    } else if (myState == State.CALL_CLOSE && opcode == Opcodes.GOTO) {
      myState = State.GOTO;
      while (myJumpsToRemove-- > 0) {
        myContext.removeLastJump();
      }
    } else if (myState == State.CALL_ADD_SUPPRESSED && opcode == Opcodes.GOTO) {
      myState = State.GOTO_2;
    } else {
      setHasInstructions();
      myState = State.INITIAL;
    }
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    if ((opcode == Opcodes.INVOKEINTERFACE || opcode == Opcodes.INVOKEVIRTUAL)
        && "close".equals(name)
        && "()V".equals(descriptor)
        && myState == State.LOAD_RESOURCE) {
      myState = State.CALL_CLOSE;
    } else if (myState == State.LOAD_ADDITIONAL_EXCEPTION
        && opcode == Opcodes.INVOKEVIRTUAL
        && "java/lang/Throwable".equals(owner)
        && "addSuppressed".equals(name)
        && "(Ljava/lang/Throwable;)V".equals(descriptor)) {
      myState = State.CALL_ADD_SUPPRESSED;
    } else {
      setHasInstructions();
      myState = State.INITIAL;
    }
  }

  @Override
  public void visitInsn(int opcode) {
    mv.visitInsn(opcode);
    if (myState == State.LOAD_INITIAL_EXCEPTION_2 && opcode == Opcodes.ATHROW) {
      myState = State.THROW;
    } else {
      setHasInstructions();
      myState = State.INITIAL;
    }
  }
}
