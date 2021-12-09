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

import com.intellij.rt.coverage.data.*;
import com.intellij.rt.coverage.instrumentation.filters.FilterUtils;
import com.intellij.rt.coverage.instrumentation.filters.classFilter.PrivateConstructorOfUtilClassFilter;
import com.intellij.rt.coverage.util.ClassNameUtil;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.org.objectweb.asm.*;

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

  private final TIntObjectHashMap<String> myNSourceLines = new TIntObjectHashMap<String>();
  private final TIntObjectHashMap<JumpsAndSwitches> myJumpsPerLine;
  private final Set<String> myMethodsWithSourceCode = new HashSet<String>();
  private int myTotalBranches = 0;
  private boolean myInterface;
  private boolean myEnum;
  private String myClassName;
  private FileMapData[] myFileMapData;

  public SourceLineCounter(final ClassData classData, final ProjectData projectData, boolean calculateJumpsPerLine) {
    this(classData, false, projectData, FilterUtils.ignorePrivateConstructorOfUtilClassEnabled(), calculateJumpsPerLine);
  }

  /**
   * Create analyser of class that had not been unloaded during coverage execution.
   * @param classData class data to store source data if <code>projectData</code> is not null
   * @param excludeLines exclude lines for init/clinit methods in case of not null <code>classData</code>
   * @param projectData source data should be collected only if not null
   * @param ignorePrivateConstructorOfUtilClasses a flag to filter out private constructor of util classes
   * @param calculateJumpsPerLine a flag to save jump data per each line, or just calculate total number of jumps
   */
  public SourceLineCounter(final ClassData classData, final boolean excludeLines,
                           final ProjectData projectData,
                           final boolean ignorePrivateConstructorOfUtilClasses,
                           final boolean calculateJumpsPerLine) {
    super(Opcodes.API_VERSION);
    ClassVisitor classVisitor = new ClassVisitor(Opcodes.API_VERSION) {};
    if (ignorePrivateConstructorOfUtilClasses) {
      classVisitor = new PrivateConstructorOfUtilClassFilter(classVisitor) {
        @Override
        protected void removeLine(int line) {
          String methodSignature = myNSourceLines.remove(line);
          if (methodSignature != null) {
            myMethodsWithSourceCode.remove(methodSignature);
          }
        }
      };
    }
    cv = classVisitor;

    myProjectData = projectData;
    myClassData = classData;
    myExcludeLines = excludeLines;
    if (calculateJumpsPerLine) {
      myJumpsPerLine = new TIntObjectHashMap<JumpsAndSwitches>();
    } else {
      myJumpsPerLine = null;
    }
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    myInterface = (access & Opcodes.ACC_INTERFACE) != 0;
    myEnum = (access & Opcodes.ACC_ENUM) != 0;
    myClassName = ClassNameUtil.convertToFQName(name);
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public void visitSource(String sourceFileName, String debug) {
    if (shouldCalculateSource()) {
      myClassData.setSource(sourceFileName);
    }
    if (debug != null && myClassName != null) {
      myFileMapData = JSR45Util.extractLineMapping(debug, myClassName);
    }
    super.visitSource(sourceFileName, debug);
  }

  public void visitOuterClass(String outerClassName, String methodName, String methodSig) {
    if (shouldCalculateSource()) {
      final String fqnName = ClassNameUtil.convertToFQName(outerClassName);
      final ClassData outerClass = myProjectData.getOrCreateClassData(fqnName);
      if (outerClass.getSource() == null) {
        outerClass.setSource(myClassData.getSource());
      }
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
    return new MethodVisitor(Opcodes.API_VERSION, v) {
      private boolean myHasInstructions;
      private int myCurrentLine = -1;


      public void visitLineNumber(final int line, final Label start) {
        super.visitLineNumber(line, start);
        removeEmptyLine();
        myHasInstructions = myNSourceLines.containsKey(line);
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
        super.visitInsn(opcode);
        if (opcode == Opcodes.RETURN && !myHasInstructions) {
          myNSourceLines.remove(myCurrentLine);
        } else {
          myHasInstructions = true;
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
        if (opcode != Opcodes.GOTO && opcode != Opcodes.JSR) {
          myTotalBranches += 2;
          if (myJumpsPerLine != null) {
            final JumpsAndSwitches jumpData = getOrCreateJumps();
            jumpData.addJump(jumpData.jumpsCount());
          }
        }
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
        myTotalBranches += labels.length;
        if (myJumpsPerLine != null) {
          final JumpsAndSwitches jumpData = getOrCreateJumps();
          int[] keys = new int[max - min + 1];
          for (int i = min; i <= max; i++) {
            keys[i - min] = i;
          }
          jumpData.addSwitch(jumpData.switchesCount(), keys);
        }
      }


      public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        myHasInstructions = true;
        myTotalBranches += labels.length;
        if (myJumpsPerLine != null) {
          final JumpsAndSwitches jumpData = getOrCreateJumps();
          jumpData.addSwitch(jumpData.switchesCount(), keys);
        }
      }


      public void visitMultiANewArrayInsn(final String desc, final int dims) {
        super.visitMultiANewArrayInsn(desc, dims);
        myHasInstructions = true;
      }

      public void visitEnd() {
        removeEmptyLine();
        super.visitEnd();
      }

      private void removeEmptyLine() {
        if (myCurrentLine != -1 && !myHasInstructions) {
          myNSourceLines.remove(myCurrentLine);
        }
      }

      private JumpsAndSwitches getOrCreateJumps() {
        if (myJumpsPerLine == null) return null;
        JumpsAndSwitches jumpData = myJumpsPerLine.get(myCurrentLine);
        if (jumpData == null) {
          jumpData = new JumpsAndSwitches();
          myJumpsPerLine.put(myCurrentLine, jumpData);
        }
        return jumpData;
      }
    };
  }

  @Override
  public void visitEnd() {
    super.visitEnd();
    applyMappings();
  }

  public int getNSourceLines() {
    return myNSourceLines.size();
  }

  public TIntObjectHashMap<String> getSourceLines() {
    return myNSourceLines;
  }

  public TIntObjectHashMap<JumpsAndSwitches> getJumpsPerLine() {
    return myJumpsPerLine;
  }


  public Set<String> getMethodsWithSourceCode() {
    return myMethodsWithSourceCode;
  }

  public int getNMethodsWithCode() {
    return myMethodsWithSourceCode.size();
  }

  public boolean isInterface() {
    return myInterface;
  }

  public boolean isEnum() {
    return myEnum;
  }

  public int getTotalBranches() {
    return myTotalBranches;
  }

  private boolean shouldCalculateSource() {
    return myProjectData != null;
  }

  private void applyMappings() {
    if (myFileMapData == null || myClassName == null) return;
    for (FileMapData mapData : myFileMapData) {
      final boolean isThisClass = myClassName.equals(mapData.getClassName());
      for (LineMapData lineMapData : mapData.getLines()) {
        for (int i = lineMapData.getTargetMinLine(); i <= lineMapData.getTargetMaxLine(); i++) {
          final String signature = myNSourceLines.remove(i);
          if (signature == null) continue;
          final int sourceLine = lineMapData.getSourceLineNumber();
          if (isThisClass && !myNSourceLines.containsKey(sourceLine)) {
            myNSourceLines.put(sourceLine, signature);
          }
        }
      }
    }
  }
}
