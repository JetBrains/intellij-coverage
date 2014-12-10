/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import com.intellij.rt.coverage.data.ProjectData;
import gnu.trove.TIntObjectHashMap;
import org.jetbrains.org.objectweb.asm.*;

import java.util.HashSet;
import java.util.Set;

/**
 * @author anna
 * @since 27-Jun-2008
 */
public class SourceLineCounter extends ClassVisitor {
  private final boolean myExcludeLines;
  private final ClassData myClassData;
  private final ProjectData myProjectData;

  private final TIntObjectHashMap myNSourceLines = new TIntObjectHashMap();
  private final Set myMethodsWithSourceCode = new HashSet();
  private int myCurrentLine;
  private boolean myInterface;
  private boolean myEnum;

  public SourceLineCounter(final ClassData classData, final boolean excludeLines, final ProjectData projectData) {
    super(Opcodes.ASM5, new ClassVisitor(Opcodes.ASM5) {});
    myProjectData = projectData;
    myClassData = classData;
    myExcludeLines = excludeLines;
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    myInterface = (access & Opcodes.ACC_INTERFACE) != 0;
    myEnum = (access & Opcodes.ACC_ENUM) != 0;
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public void visitSource(String sourceFileName, String debug) {
    if (myProjectData != null) {
      myClassData.setSource(sourceFileName);
    }
    super.visitSource(sourceFileName, debug);
  }

  public void visitOuterClass(String outerClassName, String methodName, String methodSig) {
    if (myProjectData != null) {
      myProjectData.getOrCreateClassData(outerClassName).setSource(myClassData.getSource());
    }
    super.visitOuterClass(outerClassName, methodName, methodSig);
  }

  public MethodVisitor visitMethod(final int access,
                                   final String name,
                                   final String desc,
                                   final String signature,
                                   final String[] exceptions) {
    final MethodVisitor v = cv.visitMethod(access, name, desc, signature, exceptions);
    if (myInterface) return v;
    if ((access & Opcodes.ACC_BRIDGE) != 0) return v;
    if (myEnum) {
      if (name.equals("values") && desc.startsWith("()[L")) return v;
      if (name.equals("valueOf") && desc.startsWith("(Ljava/lang/String;)L")) return v;
      if (name.equals("<init>") && signature != null && signature.equals("()V")) return v;
    }
    return new MethodVisitor(Opcodes.ASM5, v) {
      private boolean myHasInstructions;


      public void visitLineNumber(final int line, final Label start) {
        myHasInstructions = false;
        myCurrentLine = line;
        if (!myExcludeLines ||
            myClassData == null ||
            myClassData.getStatus(name + desc) != null ||
            (!name.equals("<init>") && !name.equals("<clinit>"))) {
          myNSourceLines.put(line, name + desc);
          myMethodsWithSourceCode.add(name + desc);
        }
      }

      public void visitInsn(final int opcode) {
        if (myExcludeLines) {
          if (opcode == Opcodes.RETURN && !myHasInstructions) {
            myNSourceLines.remove(myCurrentLine);
          } else {
            myHasInstructions = true;
          }
        }
      }

      public void visitIntInsn(final int opcode, final int operand) {
        super.visitIntInsn(opcode, operand);
        myHasInstructions = true;
      }

      public void visitVarInsn(final int opcode, final int var) {
        super.visitVarInsn(opcode, var);
        myHasInstructions = true;
      }

      public void visitTypeInsn(final int opcode, final String type) {
        super.visitTypeInsn(opcode, type);
        myHasInstructions = true;
      }

      public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
        super.visitFieldInsn(opcode, owner, name, desc);
        myHasInstructions = true;
      }

      public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        super.visitMethodInsn(opcode, owner, name, desc, itf);
        myHasInstructions = true;
      }

      public void visitJumpInsn(final int opcode, final Label label) {
        super.visitJumpInsn(opcode, label);
        myHasInstructions = true;
      }

      public void visitLdcInsn(final Object cst) {
        super.visitLdcInsn(cst);
        myHasInstructions = true;
      }

     
      public void visitIincInsn(final int var, final int increment) {
        super.visitIincInsn(var, increment);
        myHasInstructions = true;
      }

     
      public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label[] labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        myHasInstructions = true;
      }

     
      public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        myHasInstructions = true;
      }

     
      public void visitMultiANewArrayInsn(final String desc, final int dims) {
        super.visitMultiANewArrayInsn(desc, dims);
        myHasInstructions = true;
      }

     
      public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        myHasInstructions = true;
      }

    };
  }

  public int getNSourceLines() {
    return myNSourceLines.size();
  }

  public TIntObjectHashMap getSourceLines() {
    return myNSourceLines;
  }


  public Set getMethodsWithSourceCode() {
    return myMethodsWithSourceCode;
  }

  public int getNMethodsWithCode() {
    return myMethodsWithSourceCode.size();
  }

  public boolean isInterface() {
    return myInterface;
  }
}
