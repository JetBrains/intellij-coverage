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

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.util.StringsPool;
import org.jetbrains.coverage.org.objectweb.asm.*;

public class NewSamplingInstrumenter extends ClassVisitor {
    private static final String LINE_HITS_FIELD_NAME = "__$lineHits$__";
    private boolean myVisitedStaticBlock;

    private final ProjectData myProjectData;

    private ClassData myClassData;
    private LineData[] myLines;

    private final String myClassName;
    private final String myClassNameType;

    private final boolean myShouldCalculateSource;
    private final int myMaxLineNumber;
    private boolean myProcess;

    private boolean myEnum;

    public NewSamplingInstrumenter(final ProjectData projectData,
                                   final ClassVisitor classVisitor,
                                   final ClassReader cr,
                                   final String className,
                                   final boolean shouldCalculateSource) {
        super(Opcodes.API_VERSION, classVisitor);
        myProjectData = projectData;
        myClassName = className.replace('$', '.');
        myClassNameType = className.replace(".", "/");
        myShouldCalculateSource = shouldCalculateSource;
        myMaxLineNumber = calcMaxLineNumber(cr);
        myLines = new LineData[myMaxLineNumber + 1];
    }

    private int calcMaxLineNumber(ClassReader cr) {
        final int[] maxLine = new int[] {0};
        final ClassVisitor instrumentedMethodCounter =  new ClassVisitor(Opcodes.API_VERSION) {
            private boolean myEnum;
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                myEnum = (access & Opcodes.ACC_ENUM) != 0;
                super.visit(version, access, name, signature, superName, interfaces);
            }

            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if (!shouldProcessMethod(access, name, desc, signature, myEnum)) {
                    return null;
                }
                return new MethodVisitor(Opcodes.API_VERSION) {
                    public void visitLineNumber(int line, Label start) {
                        if (maxLine[0] < line) {
                            maxLine[0] = line;
                        }
                        super.visitLineNumber(line, start);
                    }
                };
            }
        };

        cr.accept(instrumentedMethodCounter, 0);
        return maxLine[0];
    }

    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        myEnum = (access & Opcodes.ACC_ENUM) != 0;
        myProcess = (access & Opcodes.ACC_INTERFACE) == 0;
        myClassData = myProjectData.getOrCreateClassData(StringsPool.getFromPool(myClassName));
        super.visit(version, access, name, signature, superName, interfaces);
    }

    public MethodVisitor visitMethod(final int access,
                                     final String name,
                                     final String desc,
                                     final String signature,
                                     final String[] exceptions) {
        final MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

        if (mv == null || !shouldProcessMethod(access, name, desc, signature, myEnum) || name.equals("<init>")) {  
            return mv;
        }
        myProcess = true;
        final MethodVisitor visitor = new MethodVisitor(Opcodes.API_VERSION, mv) {
            public void visitLineNumber(final int line, final Label start) {
                getOrCreateLineData(line, name, desc);

                //prepare for store: load array and index
                visitFieldInsn(Opcodes.GETSTATIC, myClassNameType, LINE_HITS_FIELD_NAME, "[I");
                pushInstruction(mv, line);

                //load array
                visitFieldInsn(Opcodes.GETSTATIC, myClassNameType, LINE_HITS_FIELD_NAME, "[I");
                //index
                pushInstruction(mv, line);
                //load array[index]
                visitInsn(Opcodes.IALOAD);

                //increment
                visitInsn(Opcodes.ICONST_1);
                visitInsn(Opcodes.IADD);

                //stack: array, index, incremented value: store value in array[index]
                visitInsn(Opcodes.IASTORE);
                super.visitLineNumber(line, start);
            }
        };
        if ("<clinit>".equals(name)) {
            myVisitedStaticBlock = true;
            return new StaticBlockMethodVisitor(visitor);
        }
        return visitor;
    }

    protected void getOrCreateLineData(int line, String name, String desc) {
        if (myLines == null) {
            myLines = new LineData[myMaxLineNumber + 1];
        }

        if (myLines[line] == null) {
            myLines[line] = new LineData(line, StringsPool.getFromPool(name + desc));
        }
    }


    private boolean shouldProcessMethod(int access, String name, String desc, String signature, boolean isEnum) {
        return (access & Opcodes.ACC_BRIDGE) == 0 &&  //try to skip bridge methods 
                (access & Opcodes.ACC_ABSTRACT) == 0 && //skip abstracts; do not include interfaces without non-abstract methods in result
                !(isEnum && isDefaultEnumMethod(name, desc, signature, myClassName));
    }


    private static boolean isDefaultEnumMethod(String name, String desc, String signature, String className) {
        return name.equals("values") && desc.equals("()[L" + className + ";") ||
                name.equals("valueOf") && desc.equals("(Ljava/lang/String;)L" + className + ";") ||
                name.equals("<init>") && signature != null && signature.equals("()V");
    }


    public void visitEnd() {
        if (myProcess) {
            visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC | Opcodes.ACC_TRANSIENT, LINE_HITS_FIELD_NAME, "[I", null, null);

            if (!myVisitedStaticBlock) {
                MethodVisitor mv = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                mv = new StaticBlockMethodVisitor(mv);
                mv.visitCode();
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(2, 0);
                mv.visitEnd();
            }

            myClassData.setLines(myLines);
        }
        myLines = null;
        super.visitEnd();
    }

    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
        if (myShouldCalculateSource) {
            myProjectData.getOrCreateClassData(myClassName).setSource(source);
        }
        if (debug != null) {
            myProjectData.addLineMaps(myClassName, JSR45Util.extractLineMapping(debug, myClassName));
        }
    }

    public void visitOuterClass(String outerClassName, String methodName, String methodSig) {
        if (myShouldCalculateSource) {
            myProjectData.getOrCreateClassData(outerClassName).setSource(myClassData.getSource());
        }
        super.visitOuterClass(outerClassName, methodName, methodSig);
    }

    private class StaticBlockMethodVisitor extends MethodVisitor {
        public StaticBlockMethodVisitor(MethodVisitor mv) {
            super(Opcodes.API_VERSION, mv);
        }

        public void visitCode() {
            super.visitCode();

            visitLdcInsn(myClassName);
            pushInstruction(this, myMaxLineNumber + 1);
            visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT);

            //register line array
            visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "touchClassLines", "(Ljava/lang/String;[I)[I", false);

            //ensure same line array loaded in different class loaders
            visitFieldInsn(Opcodes.PUTSTATIC, myClassNameType, LINE_HITS_FIELD_NAME, "[I");
        }

        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(Math.max(2, maxStack), maxLocals);
        }
    }

    private static void pushInstruction(MethodVisitor mv, int operand) {
        if (operand <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, operand);
        }
        else {
            mv.visitLdcInsn(operand);
        }
    }
}
