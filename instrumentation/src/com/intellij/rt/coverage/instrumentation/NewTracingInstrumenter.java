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

import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.data.BranchDataContainer;
import com.intellij.rt.coverage.instrumentation.data.Jump;
import com.intellij.rt.coverage.instrumentation.data.Switch;
import com.intellij.rt.coverage.instrumentation.dataAccess.CoverageDataAccess;
import org.jetbrains.coverage.org.objectweb.asm.*;

public class NewTracingInstrumenter extends AbstractTracingInstrumenter {
  private static final String BRANCH_HITS_FIELD_TYPE = "[I";
  private static final String BRANCH_HITS_LOCAL_VARIABLE_NAME = "__$localBranchHits$__";

  private final CoverageDataAccess myDataAccess;

  public NewTracingInstrumenter(ProjectData projectData, ClassVisitor classVisitor, String className, boolean shouldCalculateSource, CoverageDataAccess dataAccess) {
    super(projectData, classVisitor, className, shouldCalculateSource);
    myDataAccess = dataAccess;
  }

  @Override
  public MethodVisitor createTouchCounter(final MethodVisitor mv,
                                          final BranchDataContainer branchData,
                                          final LineEnumerator enumerator,
                                          final int access,
                                          final String name,
                                          final String desc,
                                          final String className) {
    if (enumerator.hasNoLines()) {
      return myDataAccess.createMethodVisitor(mv, name, false);
    }
    final MethodVisitor visitor = new ArrayTracingMethodVisitor(mv, access, desc, enumerator) {
      public void visitCode() {
        myDataAccess.onMethodStart(mv, getOrCreateLocalVariableIndex());
        super.visitCode();
      }
    };
    return myDataAccess.createMethodVisitor(visitor, name, true);
  }

  @Override
  public void visitEnd() {
    myDataAccess.onClassEnd(this);
    super.visitEnd();
  }

  @Override
  protected void initLineData() {
    myClassData.createHitsMask(myBranchData.getSize());
    super.initLineData();
  }

  public static class ArrayTracingMethodVisitor extends LocalVariableInserter {
    private final LineEnumerator myEnumerator;

    public ArrayTracingMethodVisitor(MethodVisitor methodVisitor, int access, String descriptor, LineEnumerator enumerator) {
      super(methodVisitor, access, descriptor, BRANCH_HITS_LOCAL_VARIABLE_NAME, BRANCH_HITS_FIELD_TYPE);
      myEnumerator = enumerator;
    }

    public void visitLineNumber(final int line, final Label start) {
      final LineData lineData = myEnumerator.getInstrumenter().getLineData(line);
      if (lineData != null) {
        incrementHitById(lineData.getId());
      }
      super.visitLineNumber(line, start);
    }

    @Override
    public void visitLabel(Label label) {
      super.visitLabel(label);

      final Jump jump = myEnumerator.getBranchData().getJump(label);
      if (jump != null) {
        incrementHitById(jump.getId());
      }

      final Switch aSwitch = myEnumerator.getBranchData().getSwitch(label);
      if (aSwitch != null) {
        incrementHitById(aSwitch.getId());
      }
    }

    private void incrementHitById(int id) {
      if (id == -1) return;
      mv.visitVarInsn(Opcodes.ALOAD, getOrCreateLocalVariableIndex());
      InstrumentationUtils.pushInt(mv, id);
      InstrumentationUtils.incrementIntArrayByIndex(mv);
    }
  }
}
