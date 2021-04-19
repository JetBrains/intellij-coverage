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

package com.intellij.rt.coverage.instrumentation.filters;

import com.intellij.rt.coverage.instrumentation.filters.enumerating.LineEnumeratorFilter;
import com.intellij.rt.coverage.instrumentation.filters.enumerating.NotNullAssertionsFilter;
import com.intellij.rt.coverage.instrumentation.filters.signature.EnumMethodsFilter;
import com.intellij.rt.coverage.instrumentation.filters.signature.MethodSignatureFilter;
import com.intellij.rt.coverage.instrumentation.filters.visiting.ClosingBracesFilter;
import com.intellij.rt.coverage.instrumentation.filters.visiting.MethodVisitingFilter;
import com.intellij.rt.coverage.instrumentation.kotlin.KotlinUtils;

import java.util.List;

public class FilterUtils {
  private static final boolean ourIgnorePrivateConstructorOfUtilClass =
      "true".equals(System.getProperty("coverage.ignore.private.constructor.util.class", "false"));

  public static boolean ignorePrivateConstructorOfUtilClassEnabled() {
    return ourIgnorePrivateConstructorOfUtilClass;
  }

  public static List<MethodSignatureFilter> createSignatureFilters() {
    List<MethodSignatureFilter> result = KotlinUtils.createSignatureFilters();
    result.add(new EnumMethodsFilter());
    return result;
  }

  public static List<MethodVisitingFilter> createVisitingFilters() {
    List<MethodVisitingFilter> result = KotlinUtils.createVisitingFilters();
    result.add(new ClosingBracesFilter());
    return result;
  }

  public static List<LineEnumeratorFilter> createLineEnumeratorFilters() {
    List<LineEnumeratorFilter> result = KotlinUtils.createLineEnumeratorFilters();
    result.add(new NotNullAssertionsFilter());
    return result;
  }
}
