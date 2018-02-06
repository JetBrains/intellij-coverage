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

import com.intellij.rt.coverage.data.TestDiscoveryProjectData;
import com.intellij.rt.coverage.instrumentation.AbstractIntellijClassfileTransformer;
import com.intellij.rt.coverage.testDiscovery.instrumentation.TestDiscoveryInstrumenter;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.ClassWriter;

import java.lang.instrument.Instrumentation;

public class TestDiscoveryPremain {
  private void performPremain(Instrumentation instrumentation) {
    // initialize TestDiscoveryProjectData before instrumentation
    @SuppressWarnings("unused")
    TestDiscoveryProjectData projectData = TestDiscoveryProjectData.getProjectData();

    instrumentation.addTransformer(new AbstractIntellijClassfileTransformer() {
      @Override
      protected ClassVisitor createClassVisitor(String className, ClassLoader loader, ClassReader cr, ClassWriter cw) {
        return new TestDiscoveryInstrumenter(cw, cr, className, loader);
      }

      @Override
      protected boolean shouldExclude(String className) {
        return false;
      }

      @Override
      protected boolean shouldIncludeBootstrapClass(String className) {
        return false;
      }

      @Override
      protected void visitClassLoader(ClassLoader classLoader) {

      }

      @Override
      protected boolean isStopped() {
        return false;
      }
    });
  }

  public static void premain(String argsString, Instrumentation instrumentation) {
    new TestDiscoveryPremain().performPremain(instrumentation);
  }
}
