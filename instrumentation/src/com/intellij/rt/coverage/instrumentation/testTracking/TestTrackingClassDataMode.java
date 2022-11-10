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

package com.intellij.rt.coverage.instrumentation.testTracking;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.*;
import com.intellij.rt.coverage.instrumentation.data.BranchDataContainer;
import com.intellij.rt.coverage.instrumentation.dataAccess.CoverageDataAccess;
import com.intellij.rt.coverage.instrumentation.dataAccess.DataAccessUtil;
import com.intellij.rt.coverage.util.TestTrackingCallback;
import org.jetbrains.coverage.org.objectweb.asm.*;

public class TestTrackingClassDataMode implements TestTrackingMode {
  public TestTrackingCallback createTestTrackingCallback() {
    return new TestTrackingCallback() {
      public void clearTrace(ClassData classData) {
        classData.setTraceMask(null);
      }

      public boolean[] traceLine(final ClassData classData, final int line) {
        boolean[] linesTrace = classData.getTraceMask();
        boolean[] result = null;
        if (linesTrace == null) {
          synchronized (classData) {
            linesTrace = classData.getTraceMask();
            if (linesTrace == null) {
              linesTrace = new boolean[classData.getLines().length];
              classData.setTraceMask(linesTrace);
              result = linesTrace;
            }
          }
        }
        linesTrace[line] = true;
        return result;
      }
    };
  }

  public Instrumenter createInstrumenter(ProjectData projectData, ClassVisitor classVisitor, ClassReader cr, String className, boolean shouldCalculateSource, CoverageDataAccess dataAccess) {
    return new TestTrackingClassDataInstrumenter(projectData, classVisitor, cr, className, shouldCalculateSource, dataAccess);
  }
}

class TestTrackingClassDataInstrumenter extends BranchesInstrumenter {
  protected static final String CLASS_DATA_LOCAL_VARIABLE_NAME = "__$classDataLocal$__";

  protected final CoverageDataAccess myDataAccess;

  public TestTrackingClassDataInstrumenter(ProjectData projectData, ClassVisitor classVisitor, ClassReader cr, String className, boolean shouldCalculateSource, CoverageDataAccess dataAccess) {
    super(projectData, classVisitor, className, shouldCalculateSource, dataAccess);
    myDataAccess = DataAccessUtil.createTestTrackingDataAccess(className, cr, false);
  }

  @Override
  public MethodVisitor createTouchCounter(MethodVisitor mv,
                                          final BranchDataContainer branchData,
                                          final BranchesEnumerator enumerator,
                                          final int access,
                                          final String name,
                                          final String desc,
                                          final String className) {
    mv = super.createTouchCounter(mv, branchData, enumerator, access, name, desc, className);
    return createMethodTransformer(mv, enumerator, access, name, desc);
  }

  protected MethodVisitor createMethodTransformer(final MethodVisitor mv, BranchesEnumerator enumerator, final int access, String name, final String desc) {
    if (enumerator.hasNoLines()) {
      return myDataAccess.createMethodVisitor(mv, name, false);
    }
    final MethodVisitor visitor = new LocalVariableInserter(mv, access, desc, CLASS_DATA_LOCAL_VARIABLE_NAME, InstrumentationUtils.OBJECT_TYPE) {
      public void visitLineNumber(final int line, final Label start) {
        final LineData lineData = getLineData(line);
        if (lineData != null) {
          mv.visitVarInsn(Opcodes.ALOAD, getOrCreateLocalVariableIndex());
          InstrumentationUtils.pushInt(mv, line);
          mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "traceLine", "(" + InstrumentationUtils.OBJECT_TYPE + "I)V", false);
        }
        super.visitLineNumber(line, start);
      }

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
}

