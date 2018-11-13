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

import jdk.nashorn.internal.codegen.types.Type;

final class TestDiscoveryInstrumentationUtils {
  private TestDiscoveryInstrumentationUtils() {
  }

  /**
   * @param name      method name
   * @param signature description of method arguments and return type
   * @return generated method id
   */
  static String[] getMethodId(String name, String signature) {
    Type[] types = Type.getMethodArguments(signature);
    String[] result = new String[types.length + 2];
    result[0] = name;
    result[1] = Type.getMethodReturnType(signature).getInternalName();
    for (int i = 0; i < types.length; i++) {
      result[i + 2] = types[i].getInternalName();
    }
    return result;
  }
}
