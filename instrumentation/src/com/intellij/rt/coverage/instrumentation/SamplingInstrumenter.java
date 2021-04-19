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
import com.intellij.rt.coverage.util.LinesUtil;
import org.jetbrains.coverage.org.objectweb.asm.*;

public class SamplingInstrumenter extends Instrumenter {
  private static final String OBJECT_TYPE = "Ljava/lang/Object;";
  private static final String CLASS_DATA_LOCAL_VARIABLE_NAME = "__class__data__";

  public SamplingInstrumenter(final ProjectData projectData, ClassVisitor classVisitor, String className, boolean shouldCalculateSource) {
    super(projectData, classVisitor, className, shouldCalculateSource);
  }

  protected MethodVisitor createMethodLineEnumerator(final MethodVisitor mv,
                                                     final String name,
                                                     final String desc,
                                                     final int access,
                                                     final String signature,
                                                     final String[] exceptions) {
    return new LocalVariableInserter(mv, access, desc, CLASS_DATA_LOCAL_VARIABLE_NAME, OBJECT_TYPE) {

      public void visitLineNumber(final int line, final Label start) {
        getOrCreateLineData(line, name, desc);
        mv.visitVarInsn(Opcodes.ALOAD, getOrCreateLocalVariableIndex());
        InstrumentationUtils.pushInt(mv, line);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "touchLine", "(" + OBJECT_TYPE + "I)V", false);
        super.visitLineNumber(line, start);
      }

      public void visitCode() {
        mv.visitLdcInsn(getClassName());
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "loadClassData", "(Ljava/lang/String;)" + OBJECT_TYPE, false);
        mv.visitVarInsn(Opcodes.ASTORE, getOrCreateLocalVariableIndex());
        super.visitCode();
      }
    };
  }

  protected void initLineData() {
    final LineData[] lines = LinesUtil.calcLineArray(myMaxLineNumber, myLines);
    myClassData.initLineMask(lines);
    myClassData.setLines(lines);
  }
}
