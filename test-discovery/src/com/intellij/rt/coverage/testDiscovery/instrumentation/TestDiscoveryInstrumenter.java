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
import org.jetbrains.coverage.org.objectweb.asm.*;

import java.util.Collections;

public class TestDiscoveryInstrumenter extends ClassVisitor {
  static final int ADDED_CODE_STACK_SIZE = 6;
  private final String myClassName;
  final String myInternalClassName;
  int myClassVersion;
  private final InstrumentedMethodsFilter myMethodFilter;
  volatile boolean myInstrumentConstructors;
  private int myCurrentMethodCount;
  /**
   * Name of generated static field which holds bitmap of class methods visited during any single test run
   */
  static final String METHODS_VISITED = "__$methodsVisited$__";
  static final String METHODS_VISITED_CLASS = "[Z";
  /**
   * Name of generated static method which is called before any instrumented method
   * to ensure that {@link TestDiscoveryInstrumenter#METHODS_VISITED} is initialized.
   * Required because instrumented method may be called before static initializer, e.g.
   * <pre>
   * <code>
   * public static void main(String[] args) {
   *  new B();
   * }
   *
   * class A {
   *   static B b = new B();
   * }
   *
   * class B extends A {
   *   B() {
   *     // called before B static initializer
   *   }
   * }
   * </code>
   * </pre>
   */
  private static final String METHODS_VISITED_INIT = "__$initMethodsVisited$__";
  private final boolean myInterface;
  private boolean myCreatedMethod = false;
  private final String[][] myMethodDescriptors;

  public TestDiscoveryInstrumenter(ClassWriter classWriter, ClassReader cr, String className) {
    super(Opcodes.API_VERSION, classWriter);
    myMethodFilter = new InstrumentedMethodsFilter(className);
    myClassName = className;
    myInternalClassName = className.replace('.', '/');
    myInterface = (cr.getAccess() & Opcodes.ACC_INTERFACE) != 0;
    myMethodDescriptors = inspectClass(cr);
  }

  private String[][] inspectClass(ClassReader cr) {
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
    if ("<clinit>".equals(name) || METHODS_VISITED_INIT.equals(name)) {
      return mv;
    }

    InstrumentedMethodsFilter.Decision decision = myMethodFilter.shouldVisitMethod(access, name, desc, signature, exceptions, myInstrumentConstructors);
    if (decision != InstrumentedMethodsFilter.Decision.YES) return mv;

    return new MethodVisitor(Opcodes.API_VERSION, mv) {
      final int myMethodId = myCurrentMethodCount++;

      @Override
      public void visitCode() {
        ensureArrayInitialized(mv);
        mv.visitFieldInsn(Opcodes.GETSTATIC, getFieldClassName(), METHODS_VISITED, METHODS_VISITED_CLASS);
        pushInstruction(this, myMethodId);
        visitInsn(Opcodes.ICONST_1);
        visitInsn(Opcodes.BASTORE);

        super.visitCode();
      }
    };
  }

  /**
   * Insert call to the __$initMethodsVisited$__
   *
   * It also will create a method for interfaces which were skipped by default: for java 1.8- static methods in interfaces were not possible,
   * but if there is non-<clinit> code in the interface, then it must be 1.8+
   */
  protected void ensureArrayInitialized(MethodVisitor mv) {
    if (myInterface && !myCreatedMethod) { //java 1.8 + can create static method
      myCreatedMethod = true;
      //don't care about serialization for interfaces, class is required => allowed to create public method
      createInitFieldMethod(Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
    }

    mv.visitMethodInsn(Opcodes.INVOKESTATIC, myInternalClassName, METHODS_VISITED_INIT, "()V", false);
  }

  protected String getFieldClassName() {
    return myInternalClassName;
  }

  @Override
  public void visitEnd() {
    if (myMethodDescriptors.length > 0) {
      generateMembers();
    }
    super.visitEnd();
  }

  /**
   * Generate field with boolean array to put true if we enter a method
   */
  protected void generateMembers() {
    int access;
    if (myInterface) {
      access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC;
    }
    else {
      access = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_TRANSIENT | Opcodes.ACC_SYNTHETIC;
    }

    visitField(access, METHODS_VISITED, METHODS_VISITED_CLASS, null, null);

    if (!myInterface) {
      createInitFieldMethod(Opcodes.ACC_SYNTHETIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC);
    }
  }

  /**
   * Creates method:
   * <pre>
   * <code>
   *   `access` static void __$initMethodsVisited$__() {
   *     if (__$methodsVisited$__ == null) {
   *       __$methodsVisited$__ = new boolean[myMethodDescriptors.size()];
   *       ...
   *     }
   *   }
   * </code>
   * </pre>
   *
   * The same array will be stored in the {@link TestDiscoveryProjectData#ourProjectData} instance
   */
  private void createInitFieldMethod(final int access) {
    MethodVisitor mv = visitMethod(access, METHODS_VISITED_INIT, "()V", null, null);
    mv.visitFieldInsn(Opcodes.GETSTATIC, getFieldClassName(), METHODS_VISITED, METHODS_VISITED_CLASS);

    final Label alreadyInitialized = new Label();
    mv.visitJumpInsn(Opcodes.IFNONNULL, alreadyInitialized);

    initArray(mv);

    mv.visitLabel(alreadyInitialized);

    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(ADDED_CODE_STACK_SIZE, 0);
    mv.visitEnd();
  }

  /**
   * Pushes class name, array of boolean and method names from stack to the {@link TestDiscoveryProjectData#trace(java.lang.String, boolean[], java.lang.String[])}
   * and store result in the field {@link TestDiscoveryInstrumenter#METHODS_VISITED}
   */
  void initArray(MethodVisitor mv) {
    mv.visitLdcInsn(myClassName);
    pushInstruction(mv, myMethodDescriptors.length);
    mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN);

    pushInstruction(mv, myMethodDescriptors.length);
    mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");

    for (int i = 0; i < myMethodDescriptors.length; ++i) {
      mv.visitInsn(Opcodes.DUP);
      pushInstruction(mv, i);
      mv.visitLdcInsn(myMethodDescriptors[i]);
      mv.visitInsn(Opcodes.AASTORE);
    }

    mv.visitMethodInsn(Opcodes.INVOKESTATIC, TestDiscoveryProjectData.PROJECT_DATA_OWNER, "trace", "(Ljava/lang/String;[Z[Z[Ljava/lang/String;)[Z", false);
    mv.visitFieldInsn(Opcodes.PUTSTATIC, getFieldClassName(), METHODS_VISITED, METHODS_VISITED_CLASS);
  }

  private static void pushInstruction(MethodVisitor mv, int operand) {
    if (operand < Byte.MAX_VALUE) mv.visitIntInsn(Opcodes.BIPUSH, operand);
    else mv.visitIntInsn(Opcodes.SIPUSH, operand);
  }
}