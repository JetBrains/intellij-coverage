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
import com.intellij.rt.coverage.instrumentation.BranchesEnumerator;
import com.intellij.rt.coverage.instrumentation.CoverageRuntime;
import com.intellij.rt.coverage.instrumentation.InstrumentationUtils;
import com.intellij.rt.coverage.instrumentation.Instrumenter;
import com.intellij.rt.coverage.instrumentation.dataAccess.CoverageDataAccess;
import com.intellij.rt.coverage.instrumentation.dataAccess.DataAccessUtil;
import com.intellij.rt.coverage.util.ClassNameUtil;
import com.intellij.rt.coverage.instrumentation.util.LocalVariableInserter;
import com.intellij.rt.coverage.util.TestTrackingCallback;
import org.jetbrains.coverage.org.objectweb.asm.*;

/**
 * Instrument class code with a boolean array field.
 * Zero element of this array is used as a flag for class registration for the current running test.
 * Initially the flag is false, then during execution com.intellij.rt.coverage.data.ProjectData#registerClassForTrace(java.lang.Object) is called.
 * When the registration is successful, the flag is set to true which means that there is no need to make registration calls.
 * When the current test is ended, the flag is set to false.
 * <p>
 * N.B. load and store of zero element should be volatile. It could be done with java.lang.invoke.VarHandle#[set|get]Volatile.
 * It is available only with JDK9 which is incompatible with JDK5, so this method is not used for now.
 * If absent volatile semantic leads to errors, use com.intellij.rt.coverage.instrumentation.testTracking.TestTrackingClassDataMode instead.
 */
public class TestTrackingArrayMode implements TestTrackingMode {
  public TestTrackingCallback createTestTrackingCallback() {
    return new TestTrackingCallback() {
      public void clearTrace(ClassData classData) {
        classData.getTraceMask()[0] = false;
      }

      public boolean[] traceLine(ClassData classData, int line) {
        throw new RuntimeException("traceLine method should not be called in array test tracking mode");
      }
    };
  }

  public Instrumenter createInstrumenter(ProjectData projectData, ClassVisitor classVisitor, ClassReader cr, String className, boolean shouldSaveSource, CoverageDataAccess dataAccess) {
    return new TestTrackingArrayInstrumenter(projectData, classVisitor, cr, className, shouldSaveSource, dataAccess);
  }
}

class TestTrackingArrayInstrumenter extends TestTrackingClassDataInstrumenter {
  private static final String TRACE_MASK_LOCAL_VARIABLE_NAME = "__$traceMaskLocal$__";

  private final CoverageDataAccess myArrayDataAccess;
  private final String myInternalClassName;

  public TestTrackingArrayInstrumenter(ProjectData projectData, ClassVisitor classVisitor, ClassReader cr, String className, boolean shouldSaveSource, CoverageDataAccess dataAccess) {
    super(projectData, classVisitor, cr, className, shouldSaveSource, dataAccess);
    myInternalClassName = ClassNameUtil.convertToInternalName(className);
    myArrayDataAccess = DataAccessUtil.createTestTrackingDataAccess(className, cr, true);
  }

  protected MethodVisitor createMethodTransformer(final MethodVisitor mv, BranchesEnumerator enumerator, final int access, String name, final String desc) {
    if (enumerator.hasNoLines()) {
      return myArrayDataAccess.createMethodVisitor(super.myDataAccess.createMethodVisitor(mv, name, false), name, false);
    }
    final MethodVisitor visitor = new LocalVariableInserter(mv, access, desc, TRACE_MASK_LOCAL_VARIABLE_NAME, DataAccessUtil.TEST_MASK_ARRAY_TYPE) {
      public void visitLineNumber(final int line, final Label start) {
        LineData lineData = getLineData(line);
        if (lineData != null) {
          // load trace mask array
          mv.visitVarInsn(Opcodes.ALOAD, getLVIndex());

          // check if register method should be called. array[0] == false => register has not been called
          mv.visitInsn(Opcodes.DUP);
          mv.visitInsn(Opcodes.ICONST_0);
          mv.visitInsn(Opcodes.BALOAD);
          final Label skip = new Label();
          mv.visitJumpInsn(Opcodes.IFNE, skip);

          // call register
          mv.visitFieldInsn(Opcodes.GETSTATIC, myInternalClassName, DataAccessUtil.CLASS_DATA_NAME, InstrumentationUtils.OBJECT_TYPE);
          mv.visitMethodInsn(Opcodes.INVOKESTATIC, CoverageRuntime.COVERAGE_RUNTIME_OWNER, "registerClassForTrace", "(" + InstrumentationUtils.OBJECT_TYPE + ")V", false);

          mv.visitLabel(skip);

          // load true value, stack: array. do: array[line] = true
          InstrumentationUtils.pushInt(mv, line);
          mv.visitInsn(Opcodes.ICONST_1);
          mv.visitInsn(Opcodes.BASTORE);
        }
        super.visitLineNumber(line, start);
      }

      public void visitCode() {
        myArrayDataAccess.onMethodStart(mv, getLVIndex());
        super.visitCode();
      }
    };
    return myArrayDataAccess.createMethodVisitor(super.myDataAccess.createMethodVisitor(visitor, name, true), name, true);
  }

  @Override
  public void visitEnd() {
    myArrayDataAccess.onClassEnd(this);
    super.visitEnd();
  }

  @Override
  protected void initLineData() {
    myClassData.createTraceMask(myMaxLineNumber + 1);
    super.initLineData();
  }
}
