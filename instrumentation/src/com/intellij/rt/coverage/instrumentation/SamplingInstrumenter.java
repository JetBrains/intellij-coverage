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
import org.jetbrains.coverage.org.objectweb.asm.commons.LocalVariablesSorter;

public class SamplingInstrumenter extends Instrumenter {
  private static final String OBJECT_TYPE = "Ljava/lang/Object;";

  public SamplingInstrumenter(final ProjectData projectData, ClassVisitor classVisitor, String className, boolean shouldCalculateSource) {
    super(projectData, classVisitor, className, shouldCalculateSource);
  }

  protected MethodVisitor createMethodLineEnumerator(final MethodVisitor mv,
                                                     final String name,
                                                     final String desc,
                                                     final int access,
                                                     final String signature,
                                                     final String[] exceptions) {
    return new LocalVariablesSorter(Opcodes.API_VERSION, access, desc, mv) {
      private Label myStartLabel;
      private Label myEndLabel;
      private int myClassDataIndex;

      public void visitLabel(Label label) {
        if (myStartLabel == null) {
          myStartLabel = label;
        }
        myEndLabel = label;
        super.visitLabel(label);
      }

      public void visitLineNumber(final int line, final Label start) {
        getOrCreateLineData(line, name, desc);
        mv.visitVarInsn(Opcodes.ALOAD, myClassDataIndex);
        if (line <= Short.MAX_VALUE) {
          mv.visitIntInsn(Opcodes.SIPUSH, line);
        }
        else {
          mv.visitLdcInsn(line);
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "touchLine", "(" + OBJECT_TYPE + "I)V", false);
        super.visitLineNumber(line, start);
      }

      public void visitCode() {
        mv.visitLdcInsn(getClassName());
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "loadClassData", "(Ljava/lang/String;)" + OBJECT_TYPE, false);
        myClassDataIndex = newLocal(Type.getType(OBJECT_TYPE));
        mv.visitVarInsn(Opcodes.ASTORE, myClassDataIndex);
        super.visitCode();
      }

      public void visitMaxs(int maxStack, int maxLocals) {
        if (myStartLabel != null && myEndLabel != null) {
          mv.visitLocalVariable("__class__data__", OBJECT_TYPE, null, myStartLabel, myEndLabel, myClassDataIndex);
        }
        super.visitMaxs(maxStack, maxLocals);
      }
    };
  }

  protected void initLineData() {
    final LineData[] lines = LinesUtil.calcLineArray(myMaxLineNumber, myLines);
    myClassData.initLineMask(lines);
    myClassData.setLines(lines);
  }
}
