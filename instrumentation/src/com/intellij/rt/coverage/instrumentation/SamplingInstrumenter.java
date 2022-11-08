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
import com.intellij.rt.coverage.instrumentation.dataAccess.CoverageDataAccess;
import com.intellij.rt.coverage.instrumentation.dataAccess.DataAccessUtil;
import com.intellij.rt.coverage.util.LinesUtil;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

public class SamplingInstrumenter extends Instrumenter {
  private static final String LINE_HITS_LOCAL_VARIABLE_NAME = "__$localLineHits$__";

  private final CoverageDataAccess myDataAccess;

  public SamplingInstrumenter(final ProjectData projectData,
                              final ClassVisitor classVisitor,
                              final String className,
                              final boolean shouldCalculateSource,
                              final CoverageDataAccess dataAccess) {
    super(projectData, classVisitor, className, shouldCalculateSource);
    myDataAccess = dataAccess;
  }

  public MethodVisitor createMethodLineEnumerator(
      MethodVisitor mv,
      final String name,
      final String desc,
      final int access,
      final String signature,
      final String[] exceptions
  ) {
    mv = new LocalVariableInserter(mv, access, desc, LINE_HITS_LOCAL_VARIABLE_NAME, DataAccessUtil.HITS_ARRAY_TYPE) {

      public void visitLineNumber(final int line, final Label start) {
        getOrCreateLineData(line, name, desc);

        mv.visitVarInsn(Opcodes.ALOAD, getOrCreateLocalVariableIndex());
        InstrumentationUtils.pushInt(mv, line);
        InstrumentationUtils.incrementIntArrayByIndex(mv);

        super.visitLineNumber(line, start);
      }

      public void visitCode() {
        myDataAccess.onMethodStart(mv, getOrCreateLocalVariableIndex());
        super.visitCode();
      }
    };
    return myDataAccess.createMethodVisitor(mv, name, true);
  }

  @Override
  public void visitEnd() {
    myDataAccess.onClassEnd(this);
    super.visitEnd();
  }

  @Override
  protected void initLineData() {
    final LineData[] lines = LinesUtil.calcLineArray(myMaxLineNumber, myLines);
    myClassData.setLines(lines);
    myClassData.createHitsMask(myMaxLineNumber + 1);
  }
}
