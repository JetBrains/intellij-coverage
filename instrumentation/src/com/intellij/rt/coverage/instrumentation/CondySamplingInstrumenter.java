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

public class CondySamplingInstrumenter extends Instrumenter {
  private static final String LINE_HITS_CONST_NAME = "__$lineHits$__";
  private static final String LINE_HITS_CONST_TYPE = "[I";

  private final Handle handle = new Handle(Opcodes.H_INVOKESTATIC, "com/intellij/rt/coverage/util/CondyUtils", "getLineMask", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;)[I", false);
  private final ConstantDynamic condy = new ConstantDynamic(LINE_HITS_CONST_NAME, InstrumentationUtils.OBJECT_TYPE, handle, getClassName());

  public CondySamplingInstrumenter(final ProjectData projectData,
                                   final ClassVisitor classVisitor,
                                   final String className,
                                   final boolean shouldCalculateSource) {
    super(projectData, classVisitor, className, shouldCalculateSource);
  }

  public MethodVisitor createMethodLineEnumerator(
      final MethodVisitor mv,
      final String name,
      final String desc,
      final int access,
      final String signature,
      final String[] exceptions
  ) {
    return new NewSamplingInstrumenter.ArraySamplingMethodVisitor(mv, access, name, desc, this) {
      public void visitCode() {
        mv.visitLdcInsn(condy);
        mv.visitTypeInsn(Opcodes.CHECKCAST, LINE_HITS_CONST_TYPE);
        mv.visitVarInsn(Opcodes.ASTORE, getOrCreateLocalVariableIndex());
        super.visitCode();
      }
    };
  }

  @Override
  protected void initLineData() {
    final LineData[] lines = LinesUtil.calcLineArray(myMaxLineNumber, myLines);
    myClassData.initLineMask(lines);
    myClassData.setLines(lines);
  }
}
