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

  public Instrumenter createInstrumenter(ProjectData projectData, ClassVisitor classVisitor, ClassReader cr, String className, boolean shouldCalculateSource) {
    return new TestTrackingArrayInstrumenter(projectData, classVisitor, cr, className, shouldCalculateSource);
  }
}

class TestTrackingArrayInstrumenter extends TestTrackingClassDataInstrumenter {
  private static final String TRACE_MASK_FIELD_NAME = "__$traceMask$__";
  private static final String TRACE_MASK_FIELD_TYPE = "[Z";
  private static final String TRACE_MASK_FIELD_INIT_NAME = "__$traceMaskInit$__";
  private static final String TRACE_MASK_LOCAL_VARIABLE_NAME = "__$traceMaskLocal$__";
  private static final String CLASS_INIT = "<clinit>";

  private final ExtraFieldInstrumenter myExtraTraceMaskFieldInstrumenter;

  public TestTrackingArrayInstrumenter(ProjectData projectData, ClassVisitor classVisitor, ClassReader cr, String className, boolean shouldCalculateSource) {
    super(projectData, classVisitor, cr, className, shouldCalculateSource);
    myExtraTraceMaskFieldInstrumenter = new ExtraTraceMaskFieldTestTrackingInstrumenter(cr, className);
  }

  protected MethodVisitor createMethodTransformer(final MethodVisitor mv, LineEnumerator enumerator, final int access, String name, final String desc) {
    if (!enumerator.hasExecutableLines()) {
      if (myExtraClassDataFieldInstrumenter.isInterface() && CLASS_INIT.equals(name)) {
        final MethodVisitor mv1 = myExtraClassDataFieldInstrumenter.createMethodVisitor(this, mv, mv, name);
        return myExtraTraceMaskFieldInstrumenter.createMethodVisitor(this, mv, mv1, name);
      }
      return mv;
    }
    final MethodVisitor visitor = new LocalVariableInserter(mv, access, desc, TRACE_MASK_LOCAL_VARIABLE_NAME, TRACE_MASK_FIELD_TYPE) {
      public void visitLineNumber(final int line, final Label start) {
        LineData lineData = getLineData(line);
        if (lineData != null) {
          // load trace mask array
          mv.visitVarInsn(Opcodes.ALOAD, getOrCreateLocalVariableIndex());

          // check if register method should be called. array[0] == false => register has not been called
          mv.visitInsn(Opcodes.DUP);
          mv.visitInsn(Opcodes.ICONST_0);
          mv.visitInsn(Opcodes.BALOAD);
          final Label skip = new Label();
          mv.visitJumpInsn(Opcodes.IFNE, skip);

          // call register
          mv.visitFieldInsn(Opcodes.GETSTATIC, myExtraClassDataFieldInstrumenter.getInternalClassName(), TestTrackingClassDataInstrumenter.CLASS_DATA_FIELD_NAME, TestTrackingClassDataInstrumenter.CLASS_DATA_FIELD_TYPE);
          mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "registerClassForTrace", "(Ljava/lang/Object;)Z", false);

          // if register is successful set array[0] = true
          // it may be unsuccessful if no test is running now
          mv.visitJumpInsn(Opcodes.IFEQ, skip);
          mv.visitVarInsn(Opcodes.ALOAD, getOrCreateLocalVariableIndex());
          mv.visitInsn(Opcodes.ICONST_0);
          mv.visitInsn(Opcodes.ICONST_1);
          mv.visitInsn(Opcodes.BASTORE);

          mv.visitLabel(skip);

          // load true value, stack: array. do: array[line] = true
          InstrumentationUtils.pushInt(mv, line);
          mv.visitInsn(Opcodes.ICONST_1);
          mv.visitInsn(Opcodes.BASTORE);
        }
        super.visitLineNumber(line, start);
      }

      public void visitCode() {
        mv.visitFieldInsn(Opcodes.GETSTATIC, myExtraTraceMaskFieldInstrumenter.getInternalClassName(), TRACE_MASK_FIELD_NAME, TRACE_MASK_FIELD_TYPE);
        mv.visitVarInsn(Opcodes.ASTORE, getOrCreateLocalVariableIndex());
        super.visitCode();
      }
    };
    final MethodVisitor mv1 = myExtraClassDataFieldInstrumenter.createMethodVisitor(this, mv, visitor, name);
    return myExtraTraceMaskFieldInstrumenter.createMethodVisitor(this, mv, mv1, name);
  }

  @Override
  public void visitEnd() {
    myExtraTraceMaskFieldInstrumenter.generateMembers(this);
    super.visitEnd();
  }

  @Override
  protected void initLineData() {
    myClassData.createTraceMask(myMaxLineNumber + 1);
    super.initLineData();
  }

  private class ExtraTraceMaskFieldTestTrackingInstrumenter extends ExtraFieldInstrumenter {

    public ExtraTraceMaskFieldTestTrackingInstrumenter(ClassReader cr, String className) {
      super(cr, null, className, TRACE_MASK_FIELD_NAME, TRACE_MASK_FIELD_TYPE, TRACE_MASK_FIELD_INIT_NAME, true);
    }

    public void initField(MethodVisitor mv) {
      mv.visitLdcInsn(getClassName());

      //get trace mask array
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "getTraceMask", "(Ljava/lang/String;)[Z", false);

      //save trace mask array
      mv.visitFieldInsn(Opcodes.PUTSTATIC, myExtraTraceMaskFieldInstrumenter.getInternalClassName(), TRACE_MASK_FIELD_NAME, TRACE_MASK_FIELD_TYPE);
    }
  }
}
