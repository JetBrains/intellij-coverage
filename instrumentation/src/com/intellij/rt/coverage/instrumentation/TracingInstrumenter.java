/*
 * Copyright 2000-2021 JetBrains s.r.o.
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
import com.intellij.rt.coverage.instrumentation.dataAccess.CoverageDataAccess;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

public class TracingInstrumenter extends AbstractTracingInstrumenter {
  private final CoverageDataAccess myDataAccess;

  public TracingInstrumenter(ProjectData projectData, ClassVisitor classVisitor, String className, boolean shouldCalculateSource, CoverageDataAccess dataAccess) {
    super(projectData, classVisitor, className, shouldCalculateSource);
    myDataAccess = dataAccess;
  }

  public MethodVisitor createTouchCounter(MethodVisitor methodVisitor, BranchDataContainer branchData, LineEnumerator enumerator, int access, String name, String desc, String className) {
    if (enumerator.hasNoLines()) return methodVisitor;
    return new TouchCounter(methodVisitor, branchData, access, desc);
  }

  @Override
  public void visitEnd() {
    myDataAccess.onClassEnd(this);
    super.visitEnd();
  }

  private class TouchCounter extends LocalVariableInserter {
    public static final String CLASS_DATA_LOCAL_VARIABLE_NAME = "__$class__data$__";
    private final BranchDataContainer myBranchData;

    public TouchCounter(MethodVisitor methodVisitor, BranchDataContainer branchData, int access, String desc) {
      super(methodVisitor, access, desc, CLASS_DATA_LOCAL_VARIABLE_NAME, InstrumentationUtils.OBJECT_TYPE);
      myBranchData = branchData;
    }


    public void visitLineNumber(int line, Label start) {
      if (myBranchData.getContext().getLineData(line) != null) {
        mv.visitVarInsn(Opcodes.ALOAD, getOrCreateLocalVariableIndex());
        InstrumentationUtils.pushInt(mv, line);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "trace", "(" + InstrumentationUtils.OBJECT_TYPE + "I)V", false);
      }
      super.visitLineNumber(line, start);
    }

    public void visitLabel(Label label) {
      super.visitLabel(label);

      final Jump jump = myBranchData.getJump(label);
      if (jump != null) {
        mv.visitVarInsn(Opcodes.ALOAD, getOrCreateLocalVariableIndex());
        InstrumentationUtils.pushInt(mv, jump.getLine());
        InstrumentationUtils.pushInt(mv, jump.getIndex());
        mv.visitInsn(jump.getType() ? Opcodes.ICONST_1 : Opcodes.ICONST_0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "touchJump", "(" + InstrumentationUtils.OBJECT_TYPE + "IIZ)V", false);
      }

      final Switch aSwitch = myBranchData.getSwitch(label);
      if (aSwitch != null) {
        mv.visitVarInsn(Opcodes.ALOAD, getOrCreateLocalVariableIndex());
        InstrumentationUtils.pushInt(mv, aSwitch.getLine());
        InstrumentationUtils.pushInt(mv, aSwitch.getIndex());
        mv.visitIntInsn(Opcodes.SIPUSH, aSwitch.getKey());
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "touchSwitch", "(" + InstrumentationUtils.OBJECT_TYPE + "III)V", false);
      }
    }

    public void visitCode() {
      myDataAccess.onMethodStart(mv, getOrCreateLocalVariableIndex());
      super.visitCode();
    }
  }
}
