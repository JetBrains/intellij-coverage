/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ProjectData;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;
import org.jetbrains.coverage.org.objectweb.asm.Type;
import org.jetbrains.coverage.org.objectweb.asm.commons.LocalVariablesSorter;

public class TouchCounter extends LocalVariablesSorter implements Opcodes {
  private final LineEnumerator myEnumerator;

  private Label myStartLabel;
  private Label myEndLabel;
  private int myClassDataIndex;

  public TouchCounter(final LineEnumerator enumerator, int access, String desc) {
    super(Opcodes.API_VERSION, access, desc, enumerator.getWV());
    myEnumerator = enumerator;
  }


  public void visitLineNumber(int line, Label start) {
    mv.visitVarInsn(Opcodes.ALOAD, myClassDataIndex);
    pushIntValue(line);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "trace", "(Ljava/lang/Object;I)V", false);
    super.visitLineNumber(line, start);
  }

  public void visitLabel(Label label) {
    if (myStartLabel == null) {
      myStartLabel = label;
    }
    myEndLabel = label;

    super.visitLabel(label);

    visitPossibleJump(label);

    final LineEnumerator.Switch aSwitch = myEnumerator.getSwitch(label);
    if (aSwitch != null) {
      mv.visitVarInsn(Opcodes.ALOAD, myClassDataIndex);
      pushIntValue(aSwitch.getLine());
      pushIntValue(aSwitch.getIndex());
      mv.visitIntInsn(Opcodes.SIPUSH, aSwitch.getKey());
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "touchSwitch", "(Ljava/lang/Object;III)V", false);
    }
  }

  private void touchBranch(final boolean trueHit, final int jumpIndex, int line) {
    mv.visitVarInsn(Opcodes.ALOAD, myClassDataIndex);
    pushIntValue(line);
    pushIntValue(jumpIndex);
    mv.visitInsn(trueHit ? Opcodes.ICONST_0 : Opcodes.ICONST_1);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "touchJump", "(Ljava/lang/Object;IIZ)V", false);
  }

  private void visitPossibleJump(Label label) {
    final LineEnumerator.Jump jump = myEnumerator.getJump(label);
    if (jump != null) {
      touchBranch(jump.getType(), jump.getIndex(), jump.getLine());
    }
  }

  public void visitCode() {
    mv.visitLdcInsn(myEnumerator.getClassName());
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "loadClassData", "(Ljava/lang/String;)Ljava/lang/Object;", false);
    myClassDataIndex = newLocal(Type.getType("Ljava/lang/Object;"));
    mv.visitVarInsn(Opcodes.ASTORE, myClassDataIndex);
    super.visitCode();
  }

  private void pushIntValue(int value) {
    if (value <= Short.MAX_VALUE) {
      mv.visitIntInsn(Opcodes.SIPUSH, value);
    } else {
      mv.visitLdcInsn(value);
    }
  }

  public void visitMaxs(int maxStack, int maxLocals) {
    if (myStartLabel != null && myEndLabel != null) {
      mv.visitLocalVariable("__class__data__", "Ljava/lang/Object;", null, myStartLabel, myEndLabel, myClassDataIndex);
    }
    super.visitMaxs(maxStack, maxLocals);
  }
}
