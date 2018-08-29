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
import org.jetbrains.coverage.org.objectweb.asm.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.zip.ZipFile;

import static org.jetbrains.coverage.org.objectweb.asm.ClassReader.SKIP_FRAMES;
import static org.jetbrains.coverage.org.objectweb.asm.Opcodes.*;

public class OpenCloseFileTransformer implements ClassFileTransformer {
  private final HashMap<String, ClassTransformation> myClassTransformations = new HashMap<String, ClassTransformation>();

  public OpenCloseFileTransformer() {
    for (ClassTransformation ct : new ArrayList<ClassTransformation>() {{
      add(create(FileOutputStream.class, "(Ljava/io/File;Z)V"));
      add(create(FileInputStream.class, "(Ljava/io/File;)V"));
      add(create(RandomAccessFile.class, "(Ljava/io/File;Ljava/lang/String;)V"));
      add(create(ZipFile.class, "(Ljava/io/File;I)V"));
    }}) {
      myClassTransformations.put(ct.myClass.getName().replace('.', '/'), ct);
    }
  }

  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
    final ClassTransformation ct = myClassTransformations.get(className);
    if (ct == null) return classfileBuffer;

    ClassReader cr = new ClassReader(classfileBuffer);
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    cr.accept(new ClassVisitor(ASM6, cw) {
      @Override
      public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor base = super.visitMethod(access, name, desc, signature, exceptions);

        MethodTransformer methodTransformer = ct.methodTransformers.get(name + desc);
        if (methodTransformer == null) return base;

        return methodTransformer.createVisitor(base);
      }
    }, SKIP_FRAMES);

    System.out.println("Injected open/close file listeners into " + className);

    return cw.toByteArray();
  }

  public Class<?>[] classesToTransform() {
    List<Class<?>> classes = new LinkedList<Class<?>>();
    for (ClassTransformation t : myClassTransformations.values()) classes.add(t.myClass);
    return classes.toArray(new Class<?>[]{});
  }

  private static ClassTransformation create(Class<?> c, String ctor) {
    return new ClassTransformation(c,
        new MethodTransformer.CtorTransformer(ctor),
        new MethodTransformer.CloseTransformer("close", "()V")
    );
  }

  private static final class ClassTransformation {
    private final Map<String, MethodTransformer> methodTransformers = new HashMap<String, MethodTransformer>();
    private final Class<?> myClass;

    private ClassTransformation(Class<?> c, MethodTransformer... methodTransformers) {
      myClass = c;
      for (MethodTransformer s : methodTransformers) {
        this.methodTransformers.put(s.name + s.signature, s);
      }
    }
  }

  private abstract static class MethodTransformer {
    final String name;
    final String signature;

    MethodTransformer(String name, String signature) {
      this.name = name;
      this.signature = signature;
    }

    abstract void generate(Generator g);

    MethodVisitor createVisitor(MethodVisitor base) {
      final Generator cg = new Generator(base);
      return new MethodVisitor(ASM6, base) {
        @Override
        public void visitInsn(int opcode) {
          switch (opcode) {
            case RETURN:
            case ARETURN:
            case DRETURN:
            case FRETURN:
            case IRETURN:
            case LRETURN:
              generate(cg);
              break;
            default:
          }
          super.visitInsn(opcode);
        }
      };
    }

    private static class CtorTransformer extends MethodTransformer {
      CtorTransformer(String constructorDesc) {
        super("<init>", constructorDesc);
      }

      protected void generate(Generator g) {
        g.call(TestDiscoveryProjectData.class.getName(), "openFile", new Class[]{Object.class, File.class});
      }
    }

    private static class CloseTransformer extends MethodTransformer {
      CloseTransformer(String methodName, String desc) {
        super(methodName, desc);
      }

      protected void generate(Generator g) {
        g.call(TestDiscoveryProjectData.class.getName(), "closeFile", new Class[]{Object.class});
      }
    }

    private static class Generator extends MethodVisitor {
      Generator(MethodVisitor mv) {
        super(ASM6, mv);
      }

      private void createArray(String type, int size) {
        putConst(size);
        visitTypeInsn(ANEWARRAY, type);
      }

      private void putConst(int i) {
        if (i <= 5) {
          visitInsn(ICONST_0 + i);
        } else {
          visitLdcInsn(i);
        }
      }

      private void pushConst(Object o) {
        if (o.getClass() == Class.class) {
          o = Type.getType((Class<?>) o);
        }
        visitLdcInsn(o);
      }

      // Should produce something like
      // ClassLoader.getSystemClassLoader()
      //    .loadClass("com.intellij.rt.coverage.testDiscovery.instrumentation.TestDiscoveryProjectData")
      //    .getDeclaredMethod("open", Object.class, File.class).invoke((Object)null, this, file);
      void call(String userClassName, String userMethodName, Class<?>[] argTypes) {
        visitMethodInsn(INVOKESTATIC, "java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;", false);

        pushConst(userClassName);
        visitMethodInsn(INVOKEVIRTUAL, "java/lang/ClassLoader", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", false);

        pushConst(userMethodName);

        createArray("java/lang/Class", argTypes.length);
        for (int i = 0; i < argTypes.length; i++) {
          visitInsn(DUP);
          putConst(i);
          pushConst(argTypes[i]);
          visitInsn(AASTORE);
        }

        visitMethodInsn(
            INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod",
            "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);

        visitInsn(ACONST_NULL);

        createArray("java/lang/Object", argTypes.length);

        for (int i = 0; i < argTypes.length; i++) {
          visitInsn(DUP);
          putConst(i);
          visitIntInsn(ALOAD, i);
          visitInsn(AASTORE);
        }

        visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke",
            "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
      }
    }
  }
}