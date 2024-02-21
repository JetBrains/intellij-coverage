/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.rt.coverage.main;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

/**
 * @author anna
 * @since 25-Feb-2010
 */
public class CoveragePremain {
  public static void premain(String argsString, Instrumentation instrumentation) throws Exception {
    premain(argsString, instrumentation,
        "com.intellij.rt.coverage.instrumentation.Instrumentator");
  }

  public static void premain(String argsString, Instrumentation instrumentation, String instrumenterName) throws Exception {
    final Class<?> instrumentator = Class.forName(instrumenterName, true, CoveragePremain.class.getClassLoader());
    final Method premainMethod = instrumentator.getDeclaredMethod("premain", String.class, Instrumentation.class);
    premainMethod.invoke(null, argsString, instrumentation);
  }
}