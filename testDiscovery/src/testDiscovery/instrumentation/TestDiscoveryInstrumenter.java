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

import com.intellij.rt.coverage.data.TestDiscoveryProjectData;
import org.jetbrains.org.objectweb.asm.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * This ClassVisitor adds byte array with 'visited' flag for all methods in given class to either
 * new field or inner class, depending on {@link #INLINE_COUNTERS}
 * Also modifies class static initializer to invoke {@link TestDiscoveryProjectData#trace(String, boolean[], String[])}
 */
public class TestDiscoveryInstrumenter extends ClassVisitor {
  private static final int ADDED_CODE_STACK_SIZE = 6;
  private final String myClassName;
  private final String myInternalClassName;
  private final ClassLoader myClassLoader;
  private final String myInternalCounterClassJVMName;
  private static final String myInternalCounterClassName = "int";
  private final InstrumentedMethodsFilter myMethodFilter;
  private final String[] myMethodNames;
  private int myCurrentMethodCount;
  private boolean myVisitedStaticBlock;
  private int myClassVersion;
  private volatile Method myDefineClassMethodRef;

  private static final String METHODS_VISITED = "__$methodsVisited$__";
  private static final String METHODS_VISITED_CLASS = "[Z";
  private static final boolean INLINE_COUNTERS = System.getProperty("idea.inline.counter.fields") != null;

  TestDiscoveryInstrumenter(ClassVisitor classVisitor, ClassReader cr, String className, ClassLoader loader) {
    super(Opcodes.ASM6, classVisitor);
    myMethodFilter = new InstrumentedMethodsFilter(className);
    myClassName = className.replace('$', '.'); // for inner classes
    myInternalClassName = className.replace('.', '/');
    myInternalCounterClassJVMName = myInternalClassName + "$" + myInternalCounterClassName;
    myClassLoader = loader;
    myMethodNames = collectMethodNames(cr, className);
  }

  private String[] collectMethodNames(ClassReader cr, final String className) {
    final List<String> instrumentedMethods = new ArrayList<String>();

    final ClassVisitor instrumentedMethodCounter = new ClassVisitor(Opcodes.ASM6) {
      final InstrumentedMethodsFilter methodsFilter = new InstrumentedMethodsFilter(className);

      public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        methodsFilter.visit(version, access, name, signature, superName, interfaces);
        myClassVersion = version;
        super.visit(version, access, name, signature, superName, interfaces);
      }

      public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (methodsFilter.shouldVisitMethod(access, name, desc, signature, exceptions)) {
          if ("<init>".equals(name)) {
            final int slashPos = className.lastIndexOf('.');
            final int $Pos = className.lastIndexOf('$');
            name = className.substring(Math.max(slashPos, $Pos) + 1);
          }
          instrumentedMethods.add(name);
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
      }
    };

    cr.accept(instrumentedMethodCounter, 0);

    return instrumentedMethods.toArray(new String[0]);
  }

  private void generateInnerClassWithCounter() {
    ClassWriter cw = new ClassWriter(0);
    MethodVisitor mv;

    cw.visit(myClassVersion,
        Opcodes.ACC_STATIC + Opcodes.ACC_FINAL + Opcodes.ACC_SUPER + Opcodes.ACC_SYNTHETIC,
        myInternalCounterClassJVMName, // ?
        null,
        "java/lang/Object",
        null);

    {
      cw.visitOuterClass(myInternalClassName, myInternalCounterClassJVMName, null);

      cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC, METHODS_VISITED,
          METHODS_VISITED_CLASS, null, null);

      MethodVisitor staticBlockVisitor = cw.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
      staticBlockVisitor = new StaticBlockMethodVisitor(staticBlockVisitor);
      staticBlockVisitor.visitCode();
      staticBlockVisitor.visitInsn(Opcodes.RETURN);
      staticBlockVisitor.visitMaxs(ADDED_CODE_STACK_SIZE, 0);
      staticBlockVisitor.visitEnd();
    }

    {
      mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
      mv.visitVarInsn(Opcodes.ALOAD, 0);
      mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
          "java/lang/Object",
          "<init>",
          "()V", false);
      mv.visitInsn(Opcodes.RETURN);
      mv.visitMaxs(1, 1);
      mv.visitEnd();
    }

    cw.visitEnd();

    try {
      byte[] bytes = cw.toByteArray();
      //saveBytes(bytes, myInternalCounterClassJVMName.replace('/', '.') + ".class");

      Method defineClassMethodRef = myDefineClassMethodRef;
      if (defineClassMethodRef == null) {
        defineClassMethodRef = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, Integer.TYPE, Integer.TYPE);
        if (defineClassMethodRef != null) {
          defineClassMethodRef.setAccessible(true);
          myDefineClassMethodRef = defineClassMethodRef;
        }
      }

      defineClassMethodRef.invoke(myClassLoader, bytes, 0, bytes.length);
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    myMethodFilter.visit(version, access, name, signature, superName, interfaces);
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public MethodVisitor visitMethod(final int access,
                                   final String name,
                                   final String desc,
                                   final String signature,
                                   final String[] exceptions) {
    final MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    if (mv == null) return null;
    if ("<clinit>".equals(name)) {
      if (INLINE_COUNTERS) {
        myVisitedStaticBlock = true;
        return new StaticBlockMethodVisitor(mv);
      } else {
        return mv;
      }
    }

    if (!myMethodFilter.shouldVisitMethod(access, name, desc, signature, exceptions)) return mv;

    return new MethodVisitor(Opcodes.ASM6, mv) {
      final int myMethodId = myCurrentMethodCount++;

      public void visitCode() {
        // todo for constructor insert the code after calling 'super'
        visitFieldInsn(Opcodes.GETSTATIC, INLINE_COUNTERS ? myInternalClassName : myInternalCounterClassJVMName, METHODS_VISITED, METHODS_VISITED_CLASS);
        pushInstruction(this, myMethodId);
        visitInsn(Opcodes.ICONST_1);
        visitInsn(Opcodes.BASTORE);

        super.visitCode();
      }
    };
  }

  public void visitEnd() {
    if (INLINE_COUNTERS) {
      visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC, METHODS_VISITED,
          METHODS_VISITED_CLASS, null, null);

      if (!myVisitedStaticBlock) {
        MethodVisitor mv = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        mv = new StaticBlockMethodVisitor(mv);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(ADDED_CODE_STACK_SIZE, 0);
        mv.visitEnd();
      }
    } else {
      if (myMethodNames.length > 0) {
        generateInnerClassWithCounter();
        visitInnerClass(myInternalCounterClassJVMName, myInternalClassName, myInternalCounterClassName, Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC);
      }
    }
    super.visitEnd();
  }

  private class StaticBlockMethodVisitor extends MethodVisitor {
    StaticBlockMethodVisitor(MethodVisitor mv) {
      super(Opcodes.ASM6, mv);
    }

    public void visitCode() {
      super.visitCode();

      visitLdcInsn(myClassName);
      pushInstruction(this, myMethodNames.length);
      visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN);

      pushInstruction(this, myMethodNames.length);
      visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");

      for (int i = 0; i < myMethodNames.length; ++i) {
        visitInsn(Opcodes.DUP);
        pushInstruction(this, i);
        visitLdcInsn(myMethodNames[i]);
        visitInsn(Opcodes.AASTORE);
      }

      visitMethodInsn(Opcodes.INVOKESTATIC, TestDiscoveryProjectData.PROJECT_DATA_OWNER, "trace", "(Ljava/lang/String;[Z[Ljava/lang/String;)[Z", false);
      visitFieldInsn(Opcodes.PUTSTATIC, INLINE_COUNTERS ? myInternalClassName : myInternalCounterClassJVMName, METHODS_VISITED, METHODS_VISITED_CLASS);

      // no return here
    }

    public void visitMaxs(int maxStack, int maxLocals) {
      super.visitMaxs(Math.max(ADDED_CODE_STACK_SIZE, maxStack), maxLocals);
    }
  }

  private static void pushInstruction(MethodVisitor mv, int operand) {
    if (operand < Byte.MAX_VALUE) mv.visitIntInsn(Opcodes.BIPUSH, operand);
    else mv.visitIntInsn(Opcodes.SIPUSH, operand);
  }
}