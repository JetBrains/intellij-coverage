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

import com.intellij.rt.coverage.util.CoverageIOUtil;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.ClassWriter;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

class ClassWriterImpl extends ClassWriter {
  private static final String JAVA_LANG_OBJECT = "java/lang/Object";
  private final ClassLoader myClassLoader;
  private final Map<ClassLoader, Map<String, ClassReader>> myClassReaders;

  ClassWriterImpl(int flags, ClassLoader classLoader, Map<ClassLoader, Map<String, ClassReader>> classReaders) {
    super(flags);
    myClassLoader = classLoader;
    myClassReaders = classReaders;
  }

  protected String getCommonSuperClass(String type1, String type2) {
    try {
      ClassReader info1 = getOrLoadClassReader(type1);
      ClassReader info2 = getOrLoadClassReader(type2);
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
      info = getOrLoadClassReader(type);
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
        if (typeImplements(itf, getOrLoadClassReader(itf), interfaceName)) {
          return true;
        }
      }
      type = classReader.getSuperName();
      classReader = getOrLoadClassReader(type);
    }
    return false;
  }

  private synchronized ClassReader getOrLoadClassReader(String className) throws IOException {
    Map<String, ClassReader> loaderClassReaders = myClassReaders.get(myClassLoader);
    if (loaderClassReaders == null) {
      myClassReaders.put(myClassLoader, loaderClassReaders = new HashMap<String, ClassReader>());
    }
    ClassReader classReader = loaderClassReaders.get(className);
    if (classReader == null) {
      InputStream is = null;
      try {
        String resource = className + ".class";
        is = myClassLoader == null
            ? ClassLoader.getSystemResourceAsStream(resource)
            : myClassLoader.getResourceAsStream(resource);
        if (is == null) {
          throw new FrameComputationClassNotFoundException("Class " + className + " not found");
        }
        loaderClassReaders.put(className, classReader = new ClassReader(is));
      } finally {
        CoverageIOUtil.close(is);
      }
    }
    return classReader;
  }

  static class FrameComputationClassNotFoundException extends RuntimeException {
    public FrameComputationClassNotFoundException(String message) {
      super(message);
    }
  }
}
