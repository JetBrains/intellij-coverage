/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

import org.jetbrains.coverage.org.objectweb.asm.*;

/**
 * Instruments class with a field of the specified type.
 * Adds initialization check into every method of the class.
 */
public abstract class ExtraFieldInstrumenter extends ClassVisitor {
  protected static final int ADDED_CODE_STACK_SIZE = 6;
  private static final String CLASS_INIT = "<clinit>";
  private static final int INTERFACE_FIELD_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC;
  private static final int CLASS_FIELD_ACCESS = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_TRANSIENT | Opcodes.ACC_SYNTHETIC;
  private static final int INIT_METHOD_ACCESS = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;

  /**
   * Name of generated static field which holds coverage data.
   */
  private final String myFieldName;
  private final String myFieldType;

  /**
   * Name of generated static method which is called before any instrumented method
   * to ensure that {@link ExtraFieldInstrumenter#myFieldName} is initialized.
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
  private final String myFieldInitMethodName;
  protected final String myInternalClassName;
  private final boolean myJava8AndAbove;
  private final boolean myInterface;
  private final boolean myShouldCoverClinit;
  private boolean mySeenClinit = false;

  public ExtraFieldInstrumenter(ClassReader cr, ClassVisitor classVisitor, String className,
                                String fieldName, String fieldType, String fieldInitName,
                                boolean shouldCoverClinit) {
    super(Opcodes.API_VERSION, classVisitor);
    myFieldName = fieldName;
    myFieldType = fieldType;
    myFieldInitMethodName = fieldInitName;
    myInternalClassName = className.replace('.', '/');
    myInterface = (cr.getAccess() & Opcodes.ACC_INTERFACE) != 0;
    myJava8AndAbove = (cr.readInt(4) & 0xFFFF) >= Opcodes.V1_8;
    myShouldCoverClinit = shouldCoverClinit;
  }

  /**
   * Generate code that crete and initialize field.
   * Name and type must be consistent with constructor parameters.
   */
  public abstract void initField(MethodVisitor mv);

  /**
   * Create method visitor that ensures field initialization.
   * @param mv instrumenting method visitor
   */
  public MethodVisitor createMethodVisitor(final ClassVisitor cv,
                                           MethodVisitor mv,
                                           final String name) {
    if (mv == null) return null;
    if (CLASS_INIT.equals(name)) {
      if (myInterface && (myJava8AndAbove || myShouldCoverClinit)) {
        mv = new MethodVisitor(Opcodes.API_VERSION, mv) {
          @Override
          public void visitCode() {
            initField(mv);
            cv.visitField(INTERFACE_FIELD_ACCESS, myFieldName, myFieldType, null, null);
            mySeenClinit = true;
            super.visitCode();
          }
        };
      }
      if (!myShouldCoverClinit) {
        return mv;
      }
    }

    if (myInterface) return mv;

    return new MethodVisitor(Opcodes.API_VERSION, mv) {
      @Override
      public void visitCode() {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, myInternalClassName, myFieldInitMethodName, "()V", false);
        super.visitCode();
      }
    };
  }

  protected MethodVisitor createMethodVisitor(final MethodVisitor mv,
                                              final String name) {
    return createMethodVisitor(cv, mv, name);
  }

  /**
   * Generate field with {@link ExtraFieldInstrumenter#myFieldType} array
   */
  public void generateMembers(ClassVisitor cv) {
    if (myInterface) {
      if (!myJava8AndAbove) {
        //only java 8+ may contain non-abstract methods in interfaces
        //no need to instrument otherwise
        return;
      }

      if (mySeenClinit) {
        //field init is already added in <clinit>, e.g. if interface has constant
        //interface I {
        //  I DEFAULT = new I ();
        //}
        return;
      }

      cv.visitField(INTERFACE_FIELD_ACCESS, myFieldName, myFieldType, null, null);
      //interface has no clinit method
      //java 11 verifies that constants are initialized in clinit
      //let's generate it!
      generateExplicitClinitForInterfaces(cv);
    } else {
      cv.visitField(CLASS_FIELD_ACCESS, myFieldName, myFieldType, null, null);
      createInitFieldMethod(cv);
    }
  }

  protected void generateMembers() {
    generateMembers(cv);
  }

  /**
   * Creates method:
   * <pre>
   * <code>
   *   `access` static void {@link ExtraFieldInstrumenter#myFieldInitMethodName}() {
   *     if ({@link ExtraFieldInstrumenter#myFieldName} == null) {
   *       {@link ExtraFieldInstrumenter#myFieldName} = new {@link ExtraFieldInstrumenter#myFieldType}();
   *       ...
   *     }
   *   }
   * </code>
   * </pre>
   */
  private void createInitFieldMethod(ClassVisitor cv) {
    MethodVisitor mv = cv.visitMethod(INIT_METHOD_ACCESS, myFieldInitMethodName, "()V", null, null);
    mv.visitFieldInsn(Opcodes.GETSTATIC, myInternalClassName, myFieldName, myFieldType);

    final Label alreadyInitialized = new Label();
    mv.visitJumpInsn(Opcodes.IFNONNULL, alreadyInitialized);

    initField(mv);

    mv.visitLabel(alreadyInitialized);

    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(ADDED_CODE_STACK_SIZE, 0);
    mv.visitEnd();
  }

  private void generateExplicitClinitForInterfaces(ClassVisitor cv) {
    MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, CLASS_INIT, "()V", null, null);
    initField(mv);
    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(ADDED_CODE_STACK_SIZE, 0);
    mv.visitEnd();
  }

  protected String getInternalClassName() {
    return myInternalClassName;
  }

  public boolean isInterface() {
    return myInterface;
  }
}
