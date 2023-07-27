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
import com.intellij.rt.coverage.instrumentation.CoverageRuntime;
import com.intellij.rt.coverage.instrumentation.InstrumentationUtils;
import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.data.Key;
import com.intellij.rt.coverage.instrumentation.dataAccess.CoverageDataAccessVisitor;
import com.intellij.rt.coverage.instrumentation.dataAccess.DataAccessUtil;
import com.intellij.rt.coverage.util.TestTrackingCallback;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

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

  public ClassVisitor createInstrumenter(ClassVisitor classVisitor, InstrumentationData data) {
    return new TestTrackingArrayInstrumenter(classVisitor, data);
  }
}

class TestTrackingArrayInstrumenter extends ClassVisitor {
  private final InstrumentationData myData;
  private final CoverageDataAccessVisitor myClassDataAccess;
  private final CoverageDataAccessVisitor myArrayDataAccess;
  private int myMaxLine;

  public TestTrackingArrayInstrumenter(ClassVisitor classVisitor, InstrumentationData data) {
    super(Opcodes.API_VERSION, new CoverageDataAccessVisitor(new CoverageDataAccessVisitor(classVisitor, DataAccessUtil.createTestTrackingDataAccess(data, false)), DataAccessUtil.createTestTrackingDataAccess(data, true)));
    myData = data;
    myClassDataAccess = (CoverageDataAccessVisitor) cv.getDelegate();
    myArrayDataAccess = (CoverageDataAccessVisitor) cv;
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
    return new MethodVisitor(Opcodes.API_VERSION, methodVisitor) {
      public void visitLineNumber(final int line, final Label start) {
        LineData lineData = myData.getLineData(line);
        if (lineData != null) {
          if (line > myMaxLine) myMaxLine = line;

          // load trace mask array
          myArrayDataAccess.loadFromLocal();
          mv.visitInsn(Opcodes.DUP);
          // load ClassData
          myClassDataAccess.loadFromLocal();
          // call check register
          mv.visitMethodInsn(Opcodes.INVOKESTATIC, CoverageRuntime.COVERAGE_RUNTIME_OWNER, "checkRegister", "(" + DataAccessUtil.TEST_MASK_ARRAY_TYPE + InstrumentationUtils.OBJECT_TYPE + ")V", false);

          // load true value, stack: array. do: array[line] = true
          InstrumentationUtils.pushInt(mv, line);
          mv.visitInsn(Opcodes.ICONST_1);
          mv.visitInsn(Opcodes.BASTORE);
        }
        super.visitLineNumber(line, start);
      }
    };
  }

  @Override
  public void visitEnd() {
    super.visitEnd();
    myData.get(Key.PROJECT_DATA).getOrCreateClassData(myData.get(Key.CLASS_NAME)).createTraceMask(1 + myMaxLine);
  }
}
