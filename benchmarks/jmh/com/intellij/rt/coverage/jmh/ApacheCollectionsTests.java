/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

package com.intellij.rt.coverage.jmh;

import junit.framework.TestCase;
import junit.textui.TestRunner;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ApacheCollectionsTests {

  private static final List<String> testNames;

  static {
    List<String> names = new ArrayList<String>();
    try {
      names = getClassNames();
    } catch (Exception ignored) {
    }
    testNames = names;
  }

  public static void runTests() throws Exception {
    TestRunner runner = new TestRunner();
    for (String name : testNames) {
      runner.start(new String[]{name});
    }
  }

  private static List<String> getClassNames() throws Exception {
    List<String> classNames = new ArrayList<String>();

    ZipInputStream zip = new ZipInputStream(new FileInputStream(testJarName()));
    for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
      if (entry.getName().endsWith(".class")) {
        String className = entry.getName().replace('/', '.');
        className = className.substring(0, className.length() - ".class".length());
        Class<?> clazz = Class.forName(className);
        if (!isTestCase(clazz)) continue;
        classNames.add(className);
      }
    }
    return classNames;
  }

  private static boolean isTestCase(Class<?> clazz) {
    do {
      if (clazz == TestCase.class) return true;
      clazz = clazz.getSuperclass();
    } while (clazz != null);
    return false;
  }

  private static String testJarName() {
    String[] jars = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
    for (String jar : jars) {
      if (jar.contains("commons-collections4-4.4-tests")) return jar;
    }
    return null;
  }
}
