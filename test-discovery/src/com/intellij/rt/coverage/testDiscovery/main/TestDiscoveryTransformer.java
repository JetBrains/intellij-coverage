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

import java.util.List;
import java.util.regex.Pattern;

public class TestDiscoveryTransformer extends AbstractIntellijClassfileTransformer {
  private static final boolean COUNTERS_IN_INNER_CLASS = System.getProperty("idea.test.discovery.counters.in.inner.class") != null;

  private final List<Pattern> exclude;
  private final List<Pattern> include;

  public TestDiscoveryTransformer(List<Pattern> exclude, List<Pattern> include) {
    this.exclude = exclude;
    this.include = include;
  }

  @Override
  protected ClassVisitor createClassVisitor(String className, ClassLoader loader, ClassReader cr, ClassVisitor cw) {
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
}
