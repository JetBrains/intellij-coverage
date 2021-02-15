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

package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.util.ClassNameUtil;
import com.intellij.rt.coverage.util.CoverageIOUtil;
import com.intellij.rt.coverage.util.ErrorReporter;
import org.jetbrains.coverage.gnu.trove.THashMap;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.ClassWriter;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
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
        System.out.println("Class transformation time: " + allTime + "s for " +
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

  private byte[] transformInner(ClassLoader loader, String className, byte[] classFileBuffer) {
    if (isStopped()) {
      return null;
    }

    try {
      if (className == null) {
        return null;
      }
      if (className.endsWith(".class")) {
        className = className.substring(0, className.length() - 6);
      }
      className = ClassNameUtil.convertToFQName(className);

      //do not instrument itself
      //and do not instrument packages which are used during instrumented method invocation
      //(inside methods touch, save, etc from ProjectData)
      if (className.startsWith("com.intellij.rt.")
          || className.startsWith("java.")
          || className.startsWith("sun.")
          || className.startsWith("com.sun.")
          || className.startsWith("jdk.")
          || className.startsWith("org.jetbrains.coverage.gnu.trove.")
          || className.startsWith("org.jetbrains.coverage.org.objectweb.")) {
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
    } catch (Throwable e) {
      ErrorReporter.reportError("Error during class instrumentation: " + className, e);
    }
    return null;
  }

  //public for test
  public byte[] instrument(final byte[] classfileBuffer, String className, ClassLoader loader, boolean computeFrames) {
    final ClassReader cr = new ClassReader(classfileBuffer);
    final ClassWriter cw;
    if (computeFrames) {
      final int version = getClassFileVersion(cr);
      int flags = (version & 0xFFFF) >= Opcodes.V1_6 && version != Opcodes.V1_1 ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS;
      cw = new MyClassWriter(flags, loader);
    } else {
      cw = new MyClassWriter(ClassWriter.COMPUTE_MAXS, loader);
    }

    final ClassVisitor cv = createClassVisitor(className, loader, cr, cw);
    cr.accept(cv, 0);
    return cw.toByteArray();
  }

  protected abstract ClassVisitor createClassVisitor(String className, ClassLoader loader, ClassReader cr, ClassWriter cw);

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

  private class MyClassWriter extends ClassWriter {
    private static final String JAVA_LANG_OBJECT = "java/lang/Object";
    private final ClassLoader classLoader;

    MyClassWriter(int flags, ClassLoader classLoader) {
      super(flags);
      this.classLoader = classLoader;
    }

    protected String getCommonSuperClass(String type1, String type2) {
      try {
        ClassReader info1 = getOrLoadClassReader(type1, classLoader);
        ClassReader info2 = getOrLoadClassReader(type2, classLoader);
        String
            superType = checkImplementInterface(type1, type2, info1, info2);
        if (superType != null) return superType;
        superType = checkImplementInterface(type2, type1, info2, info1);
        if (superType != null) return superType;

        StringBuilder b1 = typeAncestors(type1, info1);
        StringBuilder b2 = typeAncestors(type2, info2);
        String result = JAVA_LANG_OBJECT;
        int end1 = b1.length();
        int end2 = b2.length();
        while (true) {
          int start1 = b1.lastIndexOf(";", end1 - 1);
          int start2 = b2.lastIndexOf(";", end2 - 1);
          if (start1 != -1 && start2 != -1 && end1 - start1 == end2 - start2) {
            String p1 = b1.substring(start1 + 1, end1);
            String p2 = b2.substring(start2 + 1, end2);
            if (p1.equals(p2)) {
              result = p1;
              end1 = start1;
              end2 = start2;
            } else {
              return result;
            }
          } else {
            return result;
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e.toString());
      }
    }

    private String checkImplementInterface(String type1, String type2, ClassReader info1, ClassReader info2) throws IOException {
      if ((info1.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
        if (typeImplements(type2, info2, type1)) {
          return type1;
        }
        return JAVA_LANG_OBJECT;
      }
      return null;
    }

    private StringBuilder typeAncestors(String type, ClassReader info) throws IOException {
      StringBuilder b = new StringBuilder();
      while (!JAVA_LANG_OBJECT.equals(type)) {
        b.append(';').append(type);
        type = info.getSuperName();
        info = getOrLoadClassReader(type, classLoader);
      }
      return b;
    }


    private boolean typeImplements(String type, ClassReader classReader, String interfaceName) throws IOException {
      while (!JAVA_LANG_OBJECT.equals(type)) {
        String[] interfaces = classReader.getInterfaces();
        for (String itf1 : interfaces) {
          if (itf1.equals(interfaceName)) {
            return true;
          }
        }
        for (String itf : interfaces) {
          if (typeImplements(itf, getOrLoadClassReader(itf, classLoader), interfaceName)) {
            return true;
          }
        }
        type = classReader.getSuperName();
        classReader = getOrLoadClassReader(type, classLoader);
      }
      return false;
    }
  }

  private synchronized ClassReader getOrLoadClassReader(String className, ClassLoader classLoader) throws IOException {
    Map<String, ClassReader> loaderClassReaders = classReaders.get(classLoader);
    if (loaderClassReaders == null) {
      classReaders.put(classLoader, loaderClassReaders = new THashMap<String, ClassReader>());
    }
    ClassReader classReader = loaderClassReaders.get(className);
    if (classReader == null) {
      InputStream is = null;
      try {
        String resource = className + ".class";
        is = classLoader == null
            ? ClassLoader.getSystemResourceAsStream(resource)
            : classLoader.getResourceAsStream(resource);
        if (is == null) {
          throw new IOException("Class " + className + " not found");
        }
        loaderClassReaders.put(className, classReader = new ClassReader(is));
      } finally {
        if (is != null) {
          is.close();
        }
      }
    }
    return classReader;
  }
}
