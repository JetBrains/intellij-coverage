/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package com.intellij.rt.coverage.testDiscovery;

import com.intellij.rt.coverage.data.ProjectData;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

public class TestDiscoveryInstrumenter extends ClassVisitor {
  private static final int ADDED_CODE_STACK_SIZE = 6;
  protected final ClassVisitor myClassVisitor;
  private final String myClassName;
  private final String myInternalClassName;
  private final InstrumentedMethodsFilter myMethodFilter;
  private final String[] myMethodNames;
  private int myCurrentMethodCount;
  private boolean myVisitedStaticBlock;

  private static final String METHODS_VISITED = "__$methodsVisited$__";
  private static final String METHODS_VISITED_CLASS = "[Z";

  public TestDiscoveryInstrumenter(ClassVisitor classVisitor, ClassReader cr, String className) {
    super(Opcodes.ASM5, classVisitor);
    myClassVisitor = classVisitor;
    myMethodFilter = new InstrumentedMethodsFilter(className);
    myClassName = className.replace('$', '.'); // for inner classes
    myInternalClassName = className.replace('.', '/');
    myMethodNames = collectMethodNames(cr, className);
  }

  private String[] collectMethodNames(ClassReader cr, final String className) {
    final List instrumentedMethods = new ArrayList();

    final ClassVisitor instrumentedMethodCounter =  new ClassVisitor(Opcodes.ASM5) {
      final InstrumentedMethodsFilter methodsFilter = new InstrumentedMethodsFilter(className);
      public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        methodsFilter.visit(version, access, name, signature, superName, interfaces);
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

    return (String[]) instrumentedMethods.toArray(new String[instrumentedMethods.size()]);
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
    if (mv == null) return mv;
    if ("<clinit>".equals(name)) {
      myVisitedStaticBlock = true;
      return new StaticBlockMethodVisitor(mv);
    }

    if (!myMethodFilter.shouldVisitMethod(access, name, desc, signature, exceptions)) return mv;

    return new MethodVisitor(Opcodes.ASM5, mv) {
      final int myMethodId = myCurrentMethodCount++;

      public void visitCode() {
        // todo for constructor insert the code after calling 'super'
        visitFieldInsn(Opcodes.GETSTATIC, myInternalClassName, METHODS_VISITED, METHODS_VISITED_CLASS);
        pushInstruction(this, myMethodId);
        visitInsn(Opcodes.ICONST_1);
        visitInsn(Opcodes.BASTORE);

        super.visitCode();
      }
    };
  }

  public void visitEnd() {
    visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC | Opcodes.ACC_TRANSIENT, METHODS_VISITED,
            METHODS_VISITED_CLASS, null, null);

    if (!myVisitedStaticBlock) {
      MethodVisitor mv = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
      mv = new StaticBlockMethodVisitor(mv);
      mv.visitCode();
      mv.visitInsn(Opcodes.RETURN);
      mv.visitMaxs(ADDED_CODE_STACK_SIZE, 0);
      mv.visitEnd();
    }
    super.visitEnd();
  }

  private class StaticBlockMethodVisitor extends MethodVisitor {
    public StaticBlockMethodVisitor(MethodVisitor mv) {
      super(Opcodes.ASM5, mv);
    }

    public void visitCode() {
      super.visitCode();

      visitLdcInsn(myClassName);
      pushInstruction(this, myMethodNames.length);
      visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BOOLEAN);

      pushInstruction(this, myMethodNames.length);
      visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/String");

      for(int i = 0; i < myMethodNames.length; ++i) {
        visitInsn(Opcodes.DUP);
        pushInstruction(this, i);
        visitLdcInsn(myMethodNames[i]);
        visitInsn(Opcodes.AASTORE);
      }

      visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "trace", "(Ljava/lang/String;[Z[Ljava/lang/String;)[Z", false);
      visitFieldInsn(Opcodes.PUTSTATIC, myInternalClassName, METHODS_VISITED, METHODS_VISITED_CLASS);

      // no return here
    }

    public void visitMaxs(int maxStack, int maxLocals) {
      final int ourMaxStack = ADDED_CODE_STACK_SIZE;

      super.visitMaxs(Math.max(ourMaxStack, maxStack), maxLocals);
    }
  }

  private static void pushInstruction(MethodVisitor mv, int operand) {
    if (operand < Byte.MAX_VALUE) mv.visitIntInsn(Opcodes.BIPUSH, operand);
    else mv.visitIntInsn(Opcodes.SIPUSH, operand);
  }
}