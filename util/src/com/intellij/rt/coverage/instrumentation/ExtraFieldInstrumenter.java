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

import com.intellij.rt.coverage.util.ClassNameUtil;
import org.jetbrains.coverage.org.objectweb.asm.*;

/**
 * Instruments class with a field of the specified type.
 * Adds initialization check into every method of the class.
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
public abstract class ExtraFieldInstrumenter extends ClassVisitor {
  protected static final int ADDED_CODE_STACK_SIZE = 6;
  private static final String CLASS_INIT = "<clinit>";
  private static final int INTERFACE_FIELD_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC;
  private static final int CLASS_FIELD_ACCESS = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_TRANSIENT | Opcodes.ACC_SYNTHETIC;

  /**
   * Name of generated static field which holds coverage data.
   */
  private final String myFieldName;
  private final String myFieldType;

  protected final String myInternalClassName;
  private final boolean myJava8AndAbove;
  private final boolean myInterface;
  private final boolean myShouldCoverClinit;
  private boolean mySeenClinit = false;

  public ExtraFieldInstrumenter(ClassReader cr, ClassVisitor classVisitor, String className,
                                String fieldName, String fieldType,
                                boolean shouldCoverClinit) {
    super(Opcodes.API_VERSION, classVisitor);
    myFieldName = fieldName;
    myFieldType = fieldType;
    myInternalClassName = ClassNameUtil.convertToInternalName(className);
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
   * @param mv method visitor without instrumentation
   * @param newMv instrumenting method visitor
   */
  public MethodVisitor createMethodVisitor(final ClassVisitor cv,
                                           final MethodVisitor mv,
                                           MethodVisitor newMv,
                                           final String name) {
    if (mv == null) return null;
    if (CLASS_INIT.equals(name)) {
      if (myInterface && (myJava8AndAbove || myShouldCoverClinit)) {
        newMv = new MethodVisitor(Opcodes.API_VERSION, newMv) {
          @Override
          public void visitCode() {
            initField(mv);
            mySeenClinit = true;
            super.visitCode();
          }
        };
      }
      if (!myShouldCoverClinit) {
        return newMv;
      }
    }

    if (myInterface) return newMv;

    return new MethodVisitor(Opcodes.API_VERSION, newMv) {
      @Override
      public void visitCode() {
        super.visitFieldInsn(Opcodes.GETSTATIC, myInternalClassName, myFieldName, myFieldType);

        final Label alreadyInitialized = new Label();
        super.visitJumpInsn(Opcodes.IFNONNULL, alreadyInitialized);

        initField(mv);

        super.visitLabel(alreadyInitialized);
        super.visitCode();
      }
    };
  }

  protected MethodVisitor createMethodVisitor(final MethodVisitor mv,
                                              MethodVisitor newMv,
                                              final String name) {
    return createMethodVisitor(this, mv, newMv, name);
  }

  /**
   * Generate field with {@link ExtraFieldInstrumenter#myFieldType} array
   */
  public void generateMembers(ClassVisitor cv) {
    if (myInterface) {
      if (!myJava8AndAbove && !mySeenClinit) {
        //only java 8+ may contain non-abstract methods in interfaces
        //no need to instrument otherwise
        return;
      }
      cv.visitField(INTERFACE_FIELD_ACCESS, myFieldName, myFieldType, null, null);

      if (mySeenClinit) {
        //field init is already added in <clinit>, e.g. if interface has constant
        //interface I {
        //  I DEFAULT = new I ();
        //}
        return;
      }

      //interface has no clinit method
      //java 11 verifies that constants are initialized in clinit
      //let's generate it!
      generateExplicitClinitForInterfaces(cv);
    } else {
      cv.visitField(CLASS_FIELD_ACCESS, myFieldName, myFieldType, null, null);
    }
  }

  protected void generateMembers() {
    generateMembers(this);
  }

  private void generateExplicitClinitForInterfaces(ClassVisitor cv) {
    MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, CLASS_INIT, "()V", null, null);
    initField(mv);
    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(ADDED_CODE_STACK_SIZE, 0);
    mv.visitEnd();
  }

  public String getInternalClassName() {
    return myInternalClassName;
  }

  public boolean isInterface() {
    return myInterface;
  }
}
