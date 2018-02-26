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

package com.intellij.rt.coverage.testDiscovery.main;

import com.intellij.rt.coverage.instrumentation.AbstractIntellijClassfileTransformer;
import com.intellij.rt.coverage.testDiscovery.instrumentation.TestDiscoveryInnerClassInstrumenter;
import com.intellij.rt.coverage.testDiscovery.instrumentation.TestDiscoveryInstrumenter;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.ClassWriter;

import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class TestDiscoveryPremain {
  private static final boolean COUNTERS_IN_INNER_CLASS = System.getProperty("idea.test.discovery.counters.in.inner.class") != null;
  @SuppressWarnings("WeakerAccess")
  public static final String INCLUDE_PATTERNS_VM_OP = "test.discovery.include.class.patterns";
  @SuppressWarnings("WeakerAccess")
  public static final String EXCLUDE_PATTERNS_VM_OP = "test.discovery.exclude.class.patterns";

  private void performPremain(Instrumentation instrumentation) {
    System.out.println("---- IntelliJ IDEA Test Discovery ---- ");

    // separated by ;
    final List<Pattern> include = patterns(INCLUDE_PATTERNS_VM_OP);
    final List<Pattern> exclude = patterns(EXCLUDE_PATTERNS_VM_OP);

    instrumentation.addTransformer(new AbstractIntellijClassfileTransformer() {
      @Override
      protected ClassVisitor createClassVisitor(String className, ClassLoader loader, ClassReader cr, ClassWriter cw) {
        return COUNTERS_IN_INNER_CLASS 
            ? new TestDiscoveryInnerClassInstrumenter(cw, cr, className, loader) 
            : new TestDiscoveryInstrumenter(cw, cr, className);
      }

      @Override
      protected boolean shouldExclude(String className) {
        for (Pattern e : exclude) {
          if (e.matcher(className).matches()) return true;
        }
        for (Pattern e : include) {
          if (e.matcher(className).matches()) return false;
        }
        // if we have any include pattern we should say exclude class here
        return !include.isEmpty();
      }

      private boolean isSystemBasedClassLoader(ClassLoader loader) {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        ClassLoader currentLoader = loader;
        while (currentLoader != null) {
          if (systemClassLoader == currentLoader) {
            return true;
          }
          currentLoader = currentLoader.getParent();
        }
        return false;
      }
    });
  }

  private static List<Pattern> patterns(String key) {
    String property = System.getProperty(key);
    if (property == null) return Collections.emptyList();
    System.out.println("Patterns from " + key);
    List<Pattern> patterns = new ArrayList<Pattern>(1);
    for (String s : property.split(";")) {
      try {
        patterns.add(Pattern.compile(s));
        System.out.println(s);
      } catch (PatternSyntaxException ex) {
        System.err.println("Problem occurred with pattern " + s);
        System.err.println(ex.getDescription());
        System.exit(1);
      }
    }
    return patterns;
  }

  public static void premain(String argsString, Instrumentation instrumentation) {
    new TestDiscoveryPremain().performPremain(instrumentation);
  }
}
