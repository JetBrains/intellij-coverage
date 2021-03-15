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
import com.intellij.rt.coverage.instrumentation.data.BranchDataContainer;
import com.intellij.rt.coverage.instrumentation.data.Jump;
import com.intellij.rt.coverage.instrumentation.data.Switch;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

public class TouchCounter extends LocalVariableInserter implements Opcodes {
  private static final String OBJECT_TYPE = "Ljava/lang/Object;";
  private static final String CLASS_DATA_LOCAL_VARIABLE_NAME = "__class__data__";
  private final BranchDataContainer myBranchData;
  private final String myClassName;

  public TouchCounter(MethodVisitor methodVisitor, BranchDataContainer branchData, int access, String desc, String className) {
    super(methodVisitor, access, desc, CLASS_DATA_LOCAL_VARIABLE_NAME, OBJECT_TYPE);
    myBranchData = branchData;
    myClassName = className;
  }


  public void visitLineNumber(int line, Label start) {
    mv.visitVarInsn(Opcodes.ALOAD, getOrCreateLocalVariableIndex());
    InstrumentationUtils.pushInt(mv, line);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "trace", "(Ljava/lang/Object;I)V", false);
    super.visitLineNumber(line, start);
  }

  public void visitLabel(Label label) {
    super.visitLabel(label);

    visitPossibleJump(label);

    final Switch aSwitch = myBranchData.getSwitch(label);
    if (aSwitch != null) {
      mv.visitVarInsn(Opcodes.ALOAD, getOrCreateLocalVariableIndex());
      InstrumentationUtils.pushInt(mv, aSwitch.getLine());
      InstrumentationUtils.pushInt(mv, aSwitch.getIndex());
      mv.visitIntInsn(Opcodes.SIPUSH, aSwitch.getKey());
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "touchSwitch", "(Ljava/lang/Object;III)V", false);
    }
  }

  private void touchBranch(final boolean trueHit, final int jumpIndex, int line) {
    mv.visitVarInsn(Opcodes.ALOAD, getOrCreateLocalVariableIndex());
    InstrumentationUtils.pushInt(mv, line);
    InstrumentationUtils.pushInt(mv, jumpIndex);
    mv.visitInsn(trueHit ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "touchJump", "(Ljava/lang/Object;IIZ)V", false);
  }

  private void visitPossibleJump(Label label) {
    final Jump jump = myBranchData.getJump(label);
    if (jump != null) {
      touchBranch(jump.getType(), jump.getIndex(), jump.getLine());
    }
  }

  public void visitCode() {
    mv.visitLdcInsn(myClassName);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "loadClassData", "(Ljava/lang/String;)Ljava/lang/Object;", false);
    mv.visitVarInsn(Opcodes.ASTORE, getOrCreateLocalVariableIndex());
    super.visitCode();
  }
}
