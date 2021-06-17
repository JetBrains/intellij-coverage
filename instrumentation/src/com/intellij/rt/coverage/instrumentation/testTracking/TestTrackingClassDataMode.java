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

  public Instrumenter createInstrumenter(ProjectData projectData, ClassVisitor classVisitor, ClassReader cr, String className, boolean shouldCalculateSource) {
    return new TestTrackingClassDataInstrumenter(projectData, classVisitor, cr, className, shouldCalculateSource);
  }
}

class TestTrackingClassDataInstrumenter extends NewTracingInstrumenter {
  protected static final String CLASS_DATA_FIELD_NAME = "__$classData$__";
  protected static final String CLASS_DATA_FIELD_TYPE = "Ljava/lang/Object;";
  private static final String CLASS_DATA_FIELD_INIT_NAME = "__$classDataInit$__";
  private static final String CLASS_DATA_LOCAL_VARIABLE_NAME = "__$classDataLocal$__";
  private static final String CLASS_INIT = "<clinit>";

  protected final ExtraFieldInstrumenter myExtraClassDataFieldInstrumenter;

  public TestTrackingClassDataInstrumenter(ProjectData projectData, ClassVisitor classVisitor, ClassReader cr, String className, boolean shouldCalculateSource) {
    super(projectData, classVisitor, cr, className, shouldCalculateSource);
    myExtraClassDataFieldInstrumenter = new ExtraClassDataFieldTestTrackingInstrumenter(cr, className);
  }

  @Override
  public MethodVisitor createTouchCounter(MethodVisitor mv,
                                          final BranchDataContainer branchData,
                                          final LineEnumerator enumerator,
                                          final int access,
                                          final String name,
                                          final String desc,
                                          final String className) {
    mv = super.createTouchCounter(mv, branchData, enumerator, access, name, desc, className);
    return createMethodTransformer(mv, enumerator, access, name, desc);
  }

  protected MethodVisitor createMethodTransformer(final MethodVisitor mv, LineEnumerator enumerator, final int access, String name, final String desc) {
    if (!enumerator.hasExecutableLines()) {
      if (myExtraClassDataFieldInstrumenter.isInterface() && CLASS_INIT.equals(name)) {
        return myExtraClassDataFieldInstrumenter.createMethodVisitor(this, mv, mv, name);
      }
      return mv;
    }
    final MethodVisitor visitor = new LocalVariableInserter(mv, access, desc, CLASS_DATA_LOCAL_VARIABLE_NAME, CLASS_DATA_FIELD_TYPE) {
      public void visitLineNumber(final int line, final Label start) {
        final LineData lineData = getLineData(line);
        if (lineData != null) {
          mv.visitVarInsn(Opcodes.ALOAD, getOrCreateLocalVariableIndex());
          InstrumentationUtils.pushInt(mv, line);
          mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "traceLine", "(Ljava/lang/Object;I)V", false);
        }
        super.visitLineNumber(line, start);
      }

      public void visitCode() {
        mv.visitFieldInsn(Opcodes.GETSTATIC, myExtraClassDataFieldInstrumenter.getInternalClassName(), CLASS_DATA_FIELD_NAME, CLASS_DATA_FIELD_TYPE);
        mv.visitVarInsn(Opcodes.ASTORE, getOrCreateLocalVariableIndex());
        super.visitCode();
      }
    };
    return myExtraClassDataFieldInstrumenter.createMethodVisitor(this, mv, visitor, name);
  }

  @Override
  public void visitEnd() {
    myExtraClassDataFieldInstrumenter.generateMembers(this);
    super.visitEnd();
  }

  private class ExtraClassDataFieldTestTrackingInstrumenter extends ExtraFieldInstrumenter {

    public ExtraClassDataFieldTestTrackingInstrumenter(ClassReader cr, String className) {
      super(cr, null, className, CLASS_DATA_FIELD_NAME, CLASS_DATA_FIELD_TYPE, CLASS_DATA_FIELD_INIT_NAME, true);
    }

    public void initField(MethodVisitor mv) {
      mv.visitLdcInsn(getClassName());

      //get ClassData
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "loadClassData", "(Ljava/lang/String;)Ljava/lang/Object;", false);

      //save ClassData
      mv.visitFieldInsn(Opcodes.PUTSTATIC, myExtraClassDataFieldInstrumenter.getInternalClassName(), CLASS_DATA_FIELD_NAME, CLASS_DATA_FIELD_TYPE);
    }
  }
}

