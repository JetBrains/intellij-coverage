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
import com.intellij.rt.coverage.instrumentation.Instrumenter;
import com.intellij.rt.coverage.instrumentation.kotlin.KotlinUtils;
import org.jetbrains.coverage.org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;

public abstract class PrivateConstructorOfUtilClassFilter extends ClassVisitor {
  private static final String KOTLIN_OBJECT_CONSTRUCTOR_DESCRIPTOR = "(" + KotlinUtils.KOTLIN_DEFAULT_CONSTRUCTOR_MARKER + ")V";

  private final Instrumenter myInstrumenter;
  private boolean myIsAbstractClass;
  private boolean myAllMethodsStatic = true;
  private boolean myIsKotlinObject = false;
  private boolean myIsKotlinClass = false;
  private boolean myConstructorIsEmpty = true;
  private List<Integer> myConstructorLines;
  private String myName;
  private String mySuperName;

  public PrivateConstructorOfUtilClassFilter(ClassVisitor classVisitor, Instrumenter context) {
    super(Opcodes.API_VERSION, classVisitor);
    myInstrumenter = context;
  }

  protected abstract void removeLine(int line);

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    myName = name;
    mySuperName = superName;
    myIsKotlinObject |= name != null && name.endsWith("$Companion");
    myIsAbstractClass = (access & Opcodes.ACC_ABSTRACT) != 0;
  }

  @Override
  public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
    myIsKotlinObject |= (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL)) != 0
        && "INSTANCE".equals(name)
        && myName.equals(Type.getType(descriptor).getInternalName());
    return super.visitField(access, name, descriptor, signature, value);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
    if (isPrivateDefaultConstructor(access, name, descriptor)
        || isKotlinObjectSyntheticConstructor(access, name, descriptor)) {
      return new EmptyConstructorVisitor(mv);
    }
    myAllMethodsStatic &= (access & Opcodes.ACC_STATIC) != 0;
    return mv;
  }

  @Override
  public void visitEnd() {
    if ((myAllMethodsStatic || myIsKotlinObject && myIsKotlinClass)
        && myConstructorIsEmpty
        && myConstructorLines != null
        && !isSealedClassConstructor()) {
      for (int line : myConstructorLines) {
        if (myIsKotlinObject && myIsKotlinClass && myInstrumenter.linesCount() <= 1) continue;
        removeLine(line);
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
    if (myInstrumenter != null && KotlinUtils.isSealedClass(myInstrumenter)) return true;
    // if a sealed class has no derived classes, it is not marked,
    // so we have to filter such a case here
    return myIsAbstractClass && myIsKotlinClass;
  }

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
          && owner != null && (owner.equals(mySuperName) || owner.equals(myName))
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


  public static PrivateConstructorOfUtilClassFilter createWithContext(ClassVisitor cv, final Instrumenter context) {
    return new PrivateConstructorOfUtilClassFilter(cv, context) {
      @Override
      protected void removeLine(int line) {
        context.removeLine(line);
      }
    };
  }
}
