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
import com.intellij.rt.coverage.instrumentation.dataAccess.CoverageDataAccessVisitor;
import com.intellij.rt.coverage.instrumentation.dataAccess.DataAccessUtil;
import com.intellij.rt.coverage.util.TestTrackingCallback;
import org.jetbrains.coverage.org.objectweb.asm.*;

/**
 * Test tracking mode that stores classData.
 */
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

  public ClassVisitor createInstrumenter(ClassVisitor classVisitor, InstrumentationData data) {
    return new TestTrackingClassDataInstrumenter(classVisitor, data);
  }
}

class TestTrackingClassDataInstrumenter extends ClassVisitor {
  private final InstrumentationData myData;
  private final CoverageDataAccessVisitor myDataAccess;

  public TestTrackingClassDataInstrumenter(ClassVisitor classVisitor, InstrumentationData data) {
    super(Opcodes.API_VERSION, new CoverageDataAccessVisitor(classVisitor, DataAccessUtil.createTestTrackingDataAccess(data, false)));
    myData = data;
    myDataAccess = (CoverageDataAccessVisitor) cv;
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
    return new MethodVisitor(Opcodes.API_VERSION, methodVisitor) {
      public void visitLineNumber(final int line, final Label start) {
        LineData lineData = myData.getLineData(line);
        if (lineData != null) {
          myDataAccess.loadFromLocal();
          InstrumentationUtils.pushInt(mv, line);
          mv.visitMethodInsn(Opcodes.INVOKESTATIC, CoverageRuntime.COVERAGE_RUNTIME_OWNER, "traceLine", "(" + InstrumentationUtils.OBJECT_TYPE + "I)V", false);
        }
        super.visitLineNumber(line, start);
      }
    };
  }
}

