/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation.filters.classFilter;

import com.intellij.rt.coverage.instrumentation.InstrumentationUtils;
import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.data.Key;
import com.intellij.rt.coverage.instrumentation.filters.KotlinUtils;
import org.jetbrains.coverage.org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Ignore private constructor of class if all other methods are static.
 * The constructor may optionally throw an exception.
 */
public class PrivateConstructorOfUtilClassFilter extends ClassFilter {
  private static final String KOTLIN_OBJECT_CONSTRUCTOR_DESCRIPTOR = "(" + KotlinUtils.KOTLIN_DEFAULT_CONSTRUCTOR_MARKER + ")V";

  private boolean myIsAbstractClass;
  private boolean myHasMethods = false;
  private boolean myAllMethodsStatic = true;
  private boolean myHasConstFields = false;
  private boolean myAllFieldsConst = true;
  private boolean myIsKotlinObject = false;
  private boolean myIsKotlinClass = false;
  private boolean myConstructorIsEmpty = true;
  private List<Integer> myConstructorLines;
  private String myName;

  @Override
  public boolean isApplicable(InstrumentationData context) {
    return true;
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    myName = name;
    myIsKotlinObject |= name != null && name.endsWith("$Companion");
    myIsAbstractClass = (access & Opcodes.ACC_ABSTRACT) != 0;
  }

  @Override
  public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
    Type fieldType = Type.getType(descriptor);
    boolean isPublicStaticFinal = (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) != 0;
    boolean isInstanceField = isPublicStaticFinal && "INSTANCE".equals(name) && myName.equals(fieldType.getInternalName());
    myIsKotlinObject |= isInstanceField;
    if (!isInstanceField) {
      boolean isPrimitive = Type.BOOLEAN <= fieldType.getSort() && fieldType.getSort() <= Type.DOUBLE;
      boolean isString = "Ljava/lang/String;".equals(descriptor);
      boolean isConstField = isPublicStaticFinal && (isPrimitive || isString);
      myHasConstFields |= isConstField;
      myAllFieldsConst &= isConstField;
    }
    return super.visitField(access, name, descriptor, signature, value);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
    if (isPrivateDefaultConstructor(access, name, descriptor)
        || isKotlinObjectSyntheticConstructor(access, name, descriptor)) {
      return new EmptyConstructorVisitor(mv);
    }
    if (!InstrumentationUtils.CLASS_INIT.equals(name)) {
      myHasMethods = true;
      myAllMethodsStatic &= (access & Opcodes.ACC_STATIC) != 0;
    }
    return mv;
  }

  @Override
  public void visitEnd() {
    if ((myAllMethodsStatic || myIsKotlinObject && myIsKotlinClass)
        && myConstructorIsEmpty
        && myConstructorLines != null
        && !isSealedClassConstructor()) {
      for (int line : myConstructorLines) {
        // Do not ignore constructor if it is the only line in the class
        boolean isKotlinObjectWithSingleLine = myIsKotlinObject && myIsKotlinClass && myContext.getLineCount() <= 1;
        // However, const fields are inlined by the compiler.
        // In such a case, object is used just as scope, so we do not need to track coverage for it.
        boolean hasOnlyConstFields = !myHasMethods && myAllFieldsConst && myHasConstFields;
        if (!isKotlinObjectWithSingleLine || hasOnlyConstFields) {
          myContext.removeLine(line);
        }
      }
    }
    super.visitEnd();
  }

  @Override
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    myIsKotlinClass |= KotlinUtils.KOTLIN_METADATA.equals(descriptor);
    return super.visitAnnotation(descriptor, visible);
  }

  /**
   * Do not filter generated sealed class private constructor, as it is unrelated to util classes
   */
  private boolean isSealedClassConstructor() {
    Boolean value = myContext.get(Key.IS_SEALED_CLASS);
    if (value != null && value) return true;
    // if a sealed class has no derived classes, it is not marked,
    // so we have to filter such a case here
    return myIsAbstractClass && myIsKotlinClass;
  }

  //    ALOAD 0
  //    INVOKESPECIAL java/lang/Object.<init> ()V

  //    NEW <exception>
  //    DUP
  //    INVOKESPECIAL <exception>.<init> ()V
  //    ATHROW
  private class EmptyConstructorVisitor extends MethodVisitor {
    private boolean myALoadVisited = false;
    private boolean myInvokeSpecialVisited = false;

    public EmptyConstructorVisitor(MethodVisitor methodVisitor) {
      super(Opcodes.API_VERSION, methodVisitor);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
      super.visitLineNumber(line, start);
      addLine(line);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
      super.visitVarInsn(opcode, var);
      if (opcode == Opcodes.ALOAD && var == 0) {
        myALoadVisited = true;
        return;
      }
      myConstructorIsEmpty = false;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
      if (myALoadVisited
          && opcode == Opcodes.INVOKESPECIAL
          && owner != null // it could be super call or an exception creation
          && InstrumentationUtils.CONSTRUCTOR.equals(name)
          && InstrumentationUtils.CONSTRUCTOR_DESCRIPTOR.equals(descriptor)) {
        myInvokeSpecialVisited = true;
        return;
      }
      myConstructorIsEmpty = false;
    }

    @Override
    public void visitInsn(int opcode) {
      super.visitInsn(opcode);
      if (myInvokeSpecialVisited && opcode == Opcodes.RETURN) {
        return;
      }
      if (opcode == Opcodes.ATHROW || opcode == Opcodes.DUP) {
        return;
      }
      myConstructorIsEmpty = false;
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
      super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
      myConstructorIsEmpty = false;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
      super.visitFieldInsn(opcode, owner, name, descriptor);
      myConstructorIsEmpty = false;
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
      super.visitMultiANewArrayInsn(descriptor, numDimensions);
      myConstructorIsEmpty = false;
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
      super.visitTypeInsn(opcode, type);
      if (opcode == Opcodes.NEW) {
        return;
      }
      myConstructorIsEmpty = false;
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
      super.visitIntInsn(opcode, operand);
      myConstructorIsEmpty = false;
    }

    @Override
    public void visitLdcInsn(Object value) {
      super.visitLdcInsn(value);
      myConstructorIsEmpty = false;
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
      super.visitJumpInsn(opcode, label);
      myConstructorIsEmpty = false;
    }

    @Override
    public void visitIincInsn(int var, int increment) {
      super.visitIincInsn(var, increment);
      myConstructorIsEmpty = false;
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
      super.visitLookupSwitchInsn(dflt, keys, labels);
      myConstructorIsEmpty = false;
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
      super.visitTableSwitchInsn(min, max, dflt, labels);
      myConstructorIsEmpty = false;
    }
  }

  private static boolean isPrivateDefaultConstructor(int access, String name, String descriptor) {
    return (access & Opcodes.ACC_PRIVATE) != 0 && InstrumentationUtils.CONSTRUCTOR.equals(name) && InstrumentationUtils.CONSTRUCTOR_DESCRIPTOR.equals(descriptor);
  }

  private static boolean isKotlinObjectSyntheticConstructor(int access, String name, String descriptor) {
    return (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC)) != 0 && InstrumentationUtils.CONSTRUCTOR.equals(name) && KOTLIN_OBJECT_CONSTRUCTOR_DESCRIPTOR.equals(descriptor);
  }

  private void addLine(int line) {
    if (myConstructorLines == null) {
      myConstructorLines = new ArrayList<Integer>();
    }
    myConstructorLines.add(line);
  }
}
