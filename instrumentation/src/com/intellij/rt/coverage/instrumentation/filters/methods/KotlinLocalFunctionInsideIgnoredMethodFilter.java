/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

import com.intellij.rt.coverage.instrumentation.data.FilteredMethodStorage;
import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.data.Key;
import com.intellij.rt.coverage.util.OptionsUtil;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;


/**
 * This filter processes local functions defined in ignored or deprecated methods.
 * Kotlin IR compiler creates a separate methods instead of inner class for such functions.
 * In case of several methods with the same name but different signature, it is complicated to match
 * local functions with its outer methods.
 * <p>
 * In case of several methods with the same name, this check could be false positive:
 * adding ignore annotation to a <code>foo()V</code> method, local functions for <code>foo(I)V</code> will be also ignored.
 */
public class KotlinLocalFunctionInsideIgnoredMethodFilter implements MethodFilter {

  @Override
  public boolean shouldIgnore(InstrumentationData context) {
    if (!OptionsUtil.IGNORE_LOCAL_FUNCTIONS_IN_IGNORED_METHODS) return false;
    int access = context.getMethodAccess();
    if (!((access & Opcodes.ACC_PRIVATE) != 0
        && (access & Opcodes.ACC_FINAL) != 0
        && (access & Opcodes.ACC_STATIC) != 0)) return false;
    int idx = -1;
    String name = context.getMethodName();
    String className = context.get(Key.CLASS_NAME);
    FilteredMethodStorage storage = context.getProjectContext().getFilteredStorage();
    while (true) {
      idx = name.indexOf('$', idx + 1);
      if (idx < 0) return false;
      String outerMethodName = name.substring(0, idx);
      if (storage.isMethodNameIgnored(className, outerMethodName)) {
        storage.addIgnoredMethod(className, context.getMethodName() + context.getMethodDesc());
        return true;
      }
    }
  }
}
