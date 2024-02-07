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
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.ClassWriter;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

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
      return transformInner(loader, className, classFileBuffer);
    } finally {
      ourClassCount++;
      ourTime += System.nanoTime() - s;
    }
  }

  public final byte[] transform(ClassLoader loader, String className, byte[] classFileBuffer) {
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

  private byte[] transformInner(ClassLoader loader, String className, byte[] classFileBuffer) {
    if (isStopped()) {
      return null;
    }

    try {
      return transform(loader, className, classFileBuffer);
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
