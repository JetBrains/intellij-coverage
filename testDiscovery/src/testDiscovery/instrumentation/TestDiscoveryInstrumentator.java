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

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.data.TestDiscoveryProjectData;
import com.intellij.rt.coverage.instrumentation.Instrumentator;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.ClassWriter;

import java.lang.instrument.Instrumentation;

public class TestDiscoveryInstrumentator extends Instrumentator {
  public static void premain(String argsString, Instrumentation instrumentation) throws Exception {
    new TestDiscoveryInstrumentator().performPremain(argsString, instrumentation);
  }

  @Override
  public void performPremain(String argsString, Instrumentation instrumentation) throws Exception {
    // initialize TestDiscoveryProjectData before instrumentation
    @SuppressWarnings("unused")
    TestDiscoveryProjectData projectData = TestDiscoveryProjectData.getProjectData();
    super.performPremain(argsString, instrumentation);
  }

  @Override
  protected ClassVisitor createClassVisitor(ProjectData data, String className, ClassLoader loader, boolean shouldCalculateSource, ClassReader cr, ClassWriter cw) {
    return new TestDiscoveryInstrumenter(cw, cr, className, loader);
  }
}
