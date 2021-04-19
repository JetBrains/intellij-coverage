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

import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.ClassWriter;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;

public class TestDiscoveryInnerClassInstrumenter extends TestDiscoveryInstrumenter {
  private final String myInternalCounterClassJVMName;
  private static final String myInternalCounterClassName = "int";
  
  private volatile Method myDefineClassMethodRef;
  private final ClassLoader myClassLoader;

  public TestDiscoveryInnerClassInstrumenter(ClassWriter classWriter, ClassReader cr, String className, ClassLoader loader) {
    super(classWriter, cr, className);
    myInternalCounterClassJVMName = myInternalClassName + "$" + myInternalCounterClassName;
    myClassLoader = loader;
  }

  @Override
  protected String getFieldClassName() {
    return myInternalCounterClassJVMName;
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
      staticBlockVisitor.visitCode();
      initField(staticBlockVisitor);
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


  @Override
  protected void generateMembers() {
    generateInnerClassWithCounter();
    visitInnerClass(myInternalCounterClassJVMName, myInternalClassName, myInternalCounterClassName, Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC);

  }
}
