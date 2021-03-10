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

package com.intellij.rt.coverage.testDiscovery.instrumentation;

import com.intellij.rt.coverage.data.ClassMetadata;
import com.intellij.rt.coverage.data.TestDiscoveryProjectData;
import com.intellij.rt.coverage.instrumentation.ExtraFieldInstrumenter;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.ClassWriter;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

import java.util.Collections;

public class TestDiscoveryInstrumenter extends ExtraFieldInstrumenter {
  private final String myClassName;
  int myClassVersion;
  private final InstrumentedMethodsFilter myMethodFilter;
  volatile boolean myInstrumentConstructors;
  private int myCurrentMethodCount;

  static final String METHODS_VISITED = "__$methodsVisited$__";
  static final String METHODS_VISITED_CLASS = "[Z";
  private static final String METHODS_VISITED_INIT = "__$initMethodsVisited$__";
  private final String[] myMethodNames;

  public TestDiscoveryInstrumenter(ClassWriter classWriter, ClassReader cr, String className) {
    super(cr, classWriter, className, METHODS_VISITED, METHODS_VISITED_CLASS, METHODS_VISITED_INIT, false);
    myMethodFilter = new InstrumentedMethodsFilter(className);
    myClassName = className;
    myMethodNames = inspectClass(cr);
  }

  private String[] inspectClass(ClassReader cr) {
    // calculate checksums for class
    CheckSumCalculator checksumCalculator = new CheckSumCalculator(api, myClassName);
    // collect source files of class
    SourceFilesCollector sourceFilesCollector = new SourceFilesCollector(api, checksumCalculator, myClassName);
    // collect methods to instrument (and calculate checksums for them, see CheckSumCalculator)
    InstrumentedMethodsCollector methodCollector = new InstrumentedMethodsCollector(api, sourceFilesCollector, this, myClassName);
    cr.accept(methodCollector, 0);
    TestDiscoveryProjectData.getProjectData()
        .addClassMetadata(Collections.singletonList(
            new ClassMetadata(myClassName,
                sourceFilesCollector.getSources(),
                checksumCalculator.getChecksums())));
    return methodCollector.instrumentedMethods();
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    myMethodFilter.visit(version, access, name, signature, superName, interfaces);
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public MethodVisitor visitMethod(final int access,
                                   final String name,
                                   final String desc,
                                   final String signature,
                                   final String[] exceptions) {
    final MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    if (mv == null) return null;
    if ("<clinit>".equals(name)) {
      return createMethodVisitor(mv, mv, name);
    }

    InstrumentedMethodsFilter.Decision decision = myMethodFilter.shouldVisitMethod(access, name, desc, signature, exceptions, myInstrumentConstructors);
    if (decision != InstrumentedMethodsFilter.Decision.YES) return mv;

    MethodVisitor newMv = new MethodVisitor(Opcodes.API_VERSION, mv) {
      final int myMethodId = myCurrentMethodCount++;

      @Override
      public void visitCode() {
        mv.visitFieldInsn(Opcodes.GETSTATIC, getFieldClassName(), METHODS_VISITED, METHODS_VISITED_CLASS);
        pushInstruction(this, myMethodId);
        visitInsn(Opcodes.ICONST_1);
        visitInsn(Opcodes.BASTORE);

        super.visitCode();
      }
    };
    return createMethodVisitor(mv, newMv, name);
  }

  @Override
  public void visitEnd() {
    if (myMethodNames.length > 0) {
      generateMembers();
    }
    super.visitEnd();
  }

  /**
   * Pushes class name, array of boolean and method names from stack to the {@link TestDiscoveryProjectData#trace(java.lang.String, boolean[], java.lang.String[])}
   * and store result in the field {@link TestDiscoveryInstrumenter#METHODS_VISITED}
   */
  @Override
  public void initField(MethodVisitor mv) {
    mv.visitLdcInsn(myClassName);
    pushInstruction(mv, myMethodNames.length);
    mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN);

    pushInstruction(mv, myMethodNames.length);
    mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");

    for (int i = 0; i < myMethodNames.length; ++i) {
      mv.visitInsn(Opcodes.DUP);
      pushInstruction(mv, i);
      mv.visitLdcInsn(myMethodNames[i]);
      mv.visitInsn(Opcodes.AASTORE);
    }

    mv.visitMethodInsn(Opcodes.INVOKESTATIC, TestDiscoveryProjectData.PROJECT_DATA_OWNER, "trace", "(Ljava/lang/String;[Z[Ljava/lang/String;)[Z", false);
    mv.visitFieldInsn(Opcodes.PUTSTATIC, getFieldClassName(), METHODS_VISITED, METHODS_VISITED_CLASS);
  }

  private static void pushInstruction(MethodVisitor mv, int operand) {
    if (operand < Byte.MAX_VALUE) mv.visitIntInsn(Opcodes.BIPUSH, operand);
    else mv.visitIntInsn(Opcodes.SIPUSH, operand);
  }
}
