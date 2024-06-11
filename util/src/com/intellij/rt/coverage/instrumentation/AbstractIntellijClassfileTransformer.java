/*
 * Copyright 2000-2022 JetBrains s.r.o.
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
import com.intellij.rt.coverage.util.CoverageIOUtil;
import com.intellij.rt.coverage.util.ErrorReporter;
import org.jetbrains.coverage.org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.WeakHashMap;

public abstract class AbstractIntellijClassfileTransformer implements ClassFileTransformer {
  public interface InclusionPattern {
    boolean accept(String className);
  }

  private final boolean computeFrames = computeFrames();
  private final WeakHashMap<ClassLoader, Map<String, ClassReader>> classReaders = new WeakHashMap<ClassLoader, Map<String, ClassReader>>();

  private long ourTime;
  private int ourClassCount;

  protected AbstractIntellijClassfileTransformer() {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      public void run() {
        double allTime = 1. * ourTime / CoverageIOUtil.GIGA;
        ErrorReporter.printInfo("Class transformation time: " + allTime + "s for " +
            ourClassCount + " classes or " + allTime / ourClassCount + "s per class"
        );
      }
    }));
  }

  public final byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) {
    long s = System.nanoTime();
    try {
      return transformInner(loader, className, classFileBuffer, classBeingRedefined);
    } finally {
      ourClassCount++;
      ourTime += System.nanoTime() - s;
    }
  }

  public final byte[] transform(ClassLoader loader, String className, byte[] classFileBuffer, Class<?> classBeingRedefined) {
    if (className == null) {
      return null;
    }
    className = ClassNameUtil.removeClassSuffix(className);
    className = ClassNameUtil.convertToFQName(className);

    // do not instrument itself
    // and do not instrument packages which are used during instrumented method invocation
    // (inside methods touch, save, etc. from ProjectData)
    if (className.startsWith("com.intellij.rt.")
        || className.startsWith("org.jetbrains.coverage.gnu.trove.")
        || className.startsWith("org.jetbrains.coverage.org.objectweb.")
        || isInternalJavaClass(className)) {
      return null;
    }

    if (classBeingRedefined != null && classAlreadyHasCoverage(classFileBuffer)) return null;

    if (shouldExclude(className)) return null;

    visitClassLoader(loader);

    InclusionPattern inclusionPattern = getInclusionPattern();
    if (inclusionPattern == null) {
      if (loader != null) {
        return instrument(classFileBuffer, className, loader, computeFrames);
      }
    } else if (inclusionPattern.accept(className)) {
      return instrument(classFileBuffer, className, loader, computeFrames);
    }
    return null;
  }

  private boolean classAlreadyHasCoverage(byte[] classFileBuffer) {
    final boolean[] hasCoverage = new boolean[]{false};

    new ClassReader(classFileBuffer).accept(new ClassVisitor(Opcodes.API_VERSION) {
      @Override
      public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        if ("__$hits$__".equals(name)) {
          hasCoverage[0] = true;
        }
        return super.visitField(access, name, descriptor, signature, value);
      }

      @Override
      public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new MethodVisitor(Opcodes.API_VERSION, methodVisitor) {
          @Override
          public void visitLdcInsn(Object value) {
            super.visitLdcInsn(value);
            if (value instanceof ConstantDynamic) {
              ConstantDynamic condy = (ConstantDynamic) value;
              if ("com/intellij/rt/coverage/util/CondyUtils".equals(condy.getDescriptor())) {
                hasCoverage[0] = true;
              }
            }
          }

          @Override
          public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            if ("com/intellij/rt/coverage/instrumentation/CoverageRuntime".equals(owner)) {
              hasCoverage[0] = true;
            }
          }
        };
      }
    }, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
    return hasCoverage[0];
  }

  private byte[] transformInner(ClassLoader loader, String className, byte[] classFileBuffer, Class<?> classBeingRedefined) {
    if (isStopped()) {
      return null;
    }

    try {
      return transform(loader, className, classFileBuffer, classBeingRedefined);
    } catch (ClassWriterImpl.FrameComputationClassNotFoundException e) {
      ErrorReporter.info("Error during class frame computation: " + className, e);
    } catch (Throwable e) {
      ErrorReporter.warn("Error during class instrumentation: " + className, e);
    }
    return null;
  }

  protected boolean isInternalJavaClass(String className) {
    return className.startsWith("java.")
        || className.startsWith("sun.")
        || className.startsWith("com.sun.")
        || className.startsWith("jdk.");
  }

  //public for test
  public byte[] instrument(final byte[] classfileBuffer, String className, ClassLoader loader, boolean computeFrames) {
    final ClassReader cr = new ClassReader(classfileBuffer);
    final ClassWriter cw;
    if (computeFrames) {
      final int version = getClassFileVersion(cr);
      int flags = (version & 0xFFFF) >= Opcodes.V1_6 && version != Opcodes.V1_1 ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS;
      cw = new ClassWriterImpl(flags, loader, classReaders);
    } else {
      cw = new ClassWriterImpl(ClassWriter.COMPUTE_MAXS, loader, classReaders);
    }

    final ClassVisitor cv = createClassVisitor(className, loader, cr, cw);
    if (cv == null) return null;
    cr.accept(cv, ClassReader.EXPAND_FRAMES);
    return cw.toByteArray();
  }

  protected abstract ClassVisitor createClassVisitor(String className, ClassLoader loader, ClassReader cr, ClassVisitor cw);

  protected abstract boolean shouldExclude(String className);

  protected InclusionPattern getInclusionPattern() {
    return null;
  }

  protected void visitClassLoader(ClassLoader classLoader) {

  }

  protected boolean isStopped() {
    return false;
  }

  private boolean computeFrames() {
    return System.getProperty("idea.coverage.no.frames") == null;
  }

  /**
   * Returns class file version in the {@code minor << 16 | major} format.<br/>
   * <b>Warning</b>: in classes compiled with <a href="https://openjdk.java.net/jeps/12">JEP 12's</a> {@code --enable-preview} option
   * the minor version is {@code 0xFFFF}, making the whole version negative.
   */
  private static int getClassFileVersion(ClassReader reader) {
    return reader.readInt(4);
  }
}
