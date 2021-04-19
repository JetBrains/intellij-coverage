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

package com.intellij.rt.coverage.instrumentation.filters.signature;

import com.intellij.rt.coverage.instrumentation.MethodFilteringVisitor;

public class EnumMethodsFilter implements MethodSignatureFilter {
  public boolean shouldFilter(int access, String name, String desc, String signature, String[] exceptions, MethodFilteringVisitor context) {
    return context.isEnum() && isDefaultEnumMethod(name, desc, signature, context.getClassName());
  }

  private static boolean isDefaultEnumMethod(String name, String desc, String signature, String className) {
    return name.equals("values") && desc.equals("()[L" + className + ";") ||
        name.equals("valueOf") && desc.equals("(Ljava/lang/String;)L" + className + ";") ||
        name.equals("<init>") && signature != null && signature.equals("()V");
  }
}
