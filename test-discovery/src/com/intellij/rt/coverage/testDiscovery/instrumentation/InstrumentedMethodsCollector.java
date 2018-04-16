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

import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.List;

class InstrumentedMethodsCollector extends ClassVisitor {
  private static final String INIT_METHOD_NAME = "<init>";
  private final TestDiscoveryInstrumenter instrumenter;
  private final List<String> instrumentedMethods;
  private final InstrumentedMethodsFilter methodsFilter;
  private int defaultConstructorIndex = -1;

  InstrumentedMethodsCollector(int api, ClassVisitor cv, TestDiscoveryInstrumenter instrumenter, String className) {
    super(api, cv);
    this.instrumenter = instrumenter;
    this.methodsFilter = new InstrumentedMethodsFilter(className);
    this.instrumentedMethods = new ArrayList<String>();
  }

  String[] instrumentedMethods() {
    return instrumentedMethods.toArray(new String[0]);
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    methodsFilter.visit(version, access, name, signature, superName, interfaces);
    instrumenter.myClassVersion = version;
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, final String desc,
                                   String signature, String[] exceptions) {
    InstrumentedMethodsFilter.Decision decision = methodsFilter.shouldVisitMethod(
        access, name, desc, signature, exceptions, instrumenter.myInstrumentConstructors
    );
    if (decision == InstrumentedMethodsFilter.Decision.YES) {
      instrumentedMethods.add(TestDiscoveryInstrumentationUtils.getMethodId(name, desc));
      if (INIT_METHOD_NAME.equals(name) && !instrumenter.myInstrumentConstructors) {
        instrumentAllConstructors();
      }
      return super.visitMethod(access, name, desc, signature, exceptions);
    }
    else if (decision == InstrumentedMethodsFilter.Decision.NO) {
      // no further visiting
      return null;
    }
    else {
      assert decision == InstrumentedMethodsFilter.Decision.CHECK_IS_CONSTRUCTOR_DEFAULT;
      assert INIT_METHOD_NAME.equals(name);
      return new DefaultConstructorDetectionVisitor(api, super.visitMethod(access, name, desc, signature, exceptions)) {
        @Override
        void onDecisionDone(boolean isDefault) {
          if (!isDefault) {
            instrumentAllConstructors();
            instrumentedMethods.add(TestDiscoveryInstrumentationUtils.getMethodId(INIT_METHOD_NAME, desc));
          }
          else {
            defaultConstructorIndex = instrumentedMethods.size();
          }
        }
      };
    }
  }

  private void instrumentAllConstructors() {
    instrumenter.myInstrumentConstructors = true;
    if (defaultConstructorIndex != -1) {
      instrumentedMethods.add(defaultConstructorIndex, TestDiscoveryInstrumentationUtils.getMethodId(INIT_METHOD_NAME, "()V"));
      defaultConstructorIndex = -1;
    }
  }
}
