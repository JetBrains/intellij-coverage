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

package com.intellij.rt.coverage.instrumentation.filters.methods;

import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

public class DeserializeLambdaFilter implements MethodFilter {
  public boolean shouldFilter(InstrumentationData data) {
    int access = data.getMethodAccess();
    return (access & Opcodes.ACC_STATIC) != 0
        && (access & Opcodes.ACC_SYNTHETIC) != 0
        && "$deserializeLambda$".equals(data.getMethodName())
        && "(Ljava/lang/invoke/SerializedLambda;)Ljava/lang/Object;".equals(data.getMethodDesc());
  }
}
