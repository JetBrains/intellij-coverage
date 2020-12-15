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

package com.intellij.rt.coverage.instrumentation.filters.visiting;

import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.instrumentation.Instrumenter;
import com.intellij.rt.coverage.instrumentation.kotlin.KotlinUtils;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Default interface member should be filtered out from implementer.
 * Instructions list of such method consists exactly of:
 * <ol>
 * <li>LABEL</li>
 * <li>LINENUMBER</li>
 * <li>ALOAD 0</li>
 * <li>INVOKESTATIC to INTERFACE_NAME$DefaultImpls.INTERFACE_MEMBER</li>
 * <li>RETURN</li>
 * <li>LABEL</li>
 * </ol>
 * A method is filtered out is it's instructions list matches this structure.
 */
public class KotlinImplementerDefaultInterfaceMemberFilter extends MethodVisitingFilter {
  private enum State {
    SHOULD_COVER, SHOULD_NOT_COVER, UNKNOWN
  }

  private byte matchedInstructions = 0;
  private int myLine = -1;
  private LineData myPreviousLineData;
  private State myState;

  @Override
  public boolean isApplicable(Instrumenter context) {
    return KotlinUtils.isKotlinClass(context) && context.hasInterfaces();
  }

  @Override
  public void initFilter(MethodVisitor methodVisitor, Instrumenter context) {
    super.initFilter(methodVisitor, context);
    myState = State.UNKNOWN;
  }

  private boolean completed() {
    return myState != State.UNKNOWN;
  }

  private void filter() {
    if (myPreviousLineData == null) {
      myContext.removeLine(myLine);
    }
  }

  @Override
  public void visitLabel(Label label) {
    super.visitLabel(label);
    if (completed()) return;
    if (matchedInstructions == 0) {
      matchedInstructions = 1;
    } else if (matchedInstructions == 5) {
      myState = State.SHOULD_NOT_COVER;
      filter();
    } else {
      myState = State.SHOULD_COVER;
    }
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    myPreviousLineData = myContext.getLineData(line);
    super.visitLineNumber(line, start);
    if (completed()) return;
    if (matchedInstructions == 1) {
      matchedInstructions = 2;
      myLine = line;
    } else {
      myState = State.SHOULD_COVER;
    }
  }

  @Override
  public void visitVarInsn(int opcode, int var) {
    super.visitVarInsn(opcode, var);
    if (completed()) return;
    if (matchedInstructions == 2 && opcode == Opcodes.ALOAD && var == 0) {
      matchedInstructions = 3;
    } else {
      myState = State.SHOULD_COVER;
    }
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    if (completed()) return;
    if (matchedInstructions == 3 && owner.endsWith("$DefaultImpls")) {
      matchedInstructions = 4;
    } else {
      myState = State.SHOULD_COVER;
    }
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    if (completed()) return;
    if (matchedInstructions == 4 && opcode == Opcodes.RETURN) {
      matchedInstructions = 5;
    } else {
      myState = State.SHOULD_COVER;
    }
  }
}
