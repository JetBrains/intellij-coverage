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

public class NewSamplingInstrumenter extends Instrumenter {
    private static final String LINE_HITS_FIELD_NAME = "__$lineHits$__";
    private static final String LINE_HITS_FIELD_TYPE = "[I";
    private static final String LINE_HITS_FIELD_INIT_NAME = "__$lineHitsInit$__";
    private static final String LINE_HITS_LOCAL_VARIABLE_NAME = "__$localLineHits$__";

    private final String myClassNameType;
    private final ExtraFieldInstrumenter myExtraFieldInstrumenter;

    public NewSamplingInstrumenter(final ProjectData projectData,
                                   final ClassVisitor classVisitor,
                                   final ClassReader cr,
                                   final String className,
                                   final boolean shouldCalculateSource) {
        super(projectData, classVisitor, className, shouldCalculateSource);
        myExtraFieldInstrumenter = new ExtraFieldSamplingInstrumenter(cr, className);
        myClassNameType = className.replace(".", "/");
    }

    public MethodVisitor createMethodLineEnumerator(
        final MethodVisitor mv,
        final String name,
        final String desc,
        final int access,
        final String signature,
        final String[] exceptions
    ) {
        final MethodVisitor visitor = new LocalVariableInserter(mv, access, desc, LINE_HITS_LOCAL_VARIABLE_NAME, LINE_HITS_FIELD_TYPE) {
            public void visitLineNumber(final int line, final Label start) {
                getOrCreateLineData(line, name, desc);

                mv.visitVarInsn(Opcodes.ALOAD, getOrCreateLocalVariableIndex());
                InstrumentationUtils.pushInt(mv, line);

                mv.visitInsn(Opcodes.DUP2);
                //load array[index]
                mv.visitInsn(Opcodes.IALOAD);

                //increment
                mv.visitInsn(Opcodes.ICONST_1);
                mv.visitInsn(Opcodes.IADD);

                //stack: array, index, incremented value: store value in array[index]
                mv.visitInsn(Opcodes.IASTORE);
                super.visitLineNumber(line, start);
            }

            public void visitCode() {
                mv.visitFieldInsn(Opcodes.GETSTATIC, myClassNameType, LINE_HITS_FIELD_NAME, LINE_HITS_FIELD_TYPE);
                mv.visitVarInsn(Opcodes.ASTORE, getOrCreateLocalVariableIndex());
                super.visitCode();
            }
        };
        return myExtraFieldInstrumenter.createMethodVisitor(this, mv, visitor, name);
    }

    @Override
    public void visitEnd() {
        myExtraFieldInstrumenter.generateMembers(this);
        super.visitEnd();
    }

    @Override
    protected void initLineData() {
        final LineData[] lines = LinesUtil.calcLineArray(myMaxLineNumber, myLines);
        myClassData.initLineMask(lines);
        myClassData.setLines(lines);
    }

    private class ExtraFieldSamplingInstrumenter extends ExtraFieldInstrumenter {

        public ExtraFieldSamplingInstrumenter(ClassReader cr, String className) {
            super(cr, null, className, LINE_HITS_FIELD_NAME, LINE_HITS_FIELD_TYPE, LINE_HITS_FIELD_INIT_NAME, true);
        }

        public void initField(MethodVisitor mv) {
            mv.visitLdcInsn(getClassName());

            //get line array
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "getLineMask", "(Ljava/lang/String;)[I", false);

            //save line array
            mv.visitFieldInsn(Opcodes.PUTSTATIC, myClassNameType, LINE_HITS_FIELD_NAME, LINE_HITS_FIELD_TYPE);
        }
    }
}
