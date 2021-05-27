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

import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.data.BranchDataContainer;
import org.jetbrains.coverage.org.objectweb.asm.*;

public class NewTracingTestTrackingInstrumenter extends NewTracingInstrumenter {
  private static final String CLASS_DATA_FIELD_NAME = "__$classData$__";
  private static final String CLASS_DATA_FIELD_TYPE = "Ljava/lang/Object;";
  private static final String CLASS_DATA_FIELD_INIT_NAME = "__$classDataInit$__";
  private static final String TRACE_MASK_FIELD_NAME = "__$traceMask$__";
  private static final String TRACE_MASK_FIELD_TYPE = "[Z";
  private static final String TRACE_MASK_FIELD_INIT_NAME = "__$traceMaskInit$__";
  private static final String TRACE_MASK_LOCAL_VARIABLE_NAME = "__$traceMaskLocal$__";
  private static final String CLASS_INIT = "<clinit>";

  private final ExtraFieldInstrumenter myExtraClassDataFieldInstrumenter;
  private final ExtraFieldInstrumenter myExtraTraceMaskFieldInstrumenter;

  public NewTracingTestTrackingInstrumenter(ProjectData projectData, ClassVisitor classVisitor, ClassReader cr, String className, boolean shouldCalculateSource) {
    super(projectData, classVisitor, cr, className, shouldCalculateSource);
    myExtraClassDataFieldInstrumenter = new ExtraClassDataFieldTestTrackingInstrumenter(cr, className);
    myExtraTraceMaskFieldInstrumenter = new ExtraTraceMaskFieldTestTrackingInstrumenter(cr, className);
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
    if (myExtraClassDataFieldInstrumenter.isInterface() && CLASS_INIT.equals(name)) {
      final MethodVisitor mv1 = myExtraClassDataFieldInstrumenter.createMethodVisitor(this, mv, mv, name);
      return myExtraTraceMaskFieldInstrumenter.createMethodVisitor(this, mv1, mv1, name);
    }
    if (!enumerator.hasExecutableLines()) return mv;
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
          mv.visitFieldInsn(Opcodes.GETSTATIC, myExtraClassDataFieldInstrumenter.getInternalClassName(), CLASS_DATA_FIELD_NAME, CLASS_DATA_FIELD_TYPE);
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
    myExtraClassDataFieldInstrumenter.generateMembers(this);
    myExtraTraceMaskFieldInstrumenter.generateMembers(this);
    super.visitEnd();
  }

  @Override
  protected void initLineData() {
    myClassData.createTraceMask(myMaxLineNumber + 1);
    super.initLineData();
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
