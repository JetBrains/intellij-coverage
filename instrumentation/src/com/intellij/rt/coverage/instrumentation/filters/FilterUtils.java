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

import com.intellij.rt.coverage.instrumentation.filters.branches.BranchesFilter;
import com.intellij.rt.coverage.instrumentation.filters.branches.NotNullAssertionsFilter;
import com.intellij.rt.coverage.instrumentation.filters.classes.ClassFilter;
import com.intellij.rt.coverage.instrumentation.filters.lines.AnnotationIgnoredMethodFilter;
import com.intellij.rt.coverage.instrumentation.filters.lines.ClosingBracesFilter;
import com.intellij.rt.coverage.instrumentation.filters.lines.LinesFilter;
import com.intellij.rt.coverage.instrumentation.filters.methods.DeserializeLambdaFilter;
import com.intellij.rt.coverage.instrumentation.filters.methods.EnumMethodsFilter;
import com.intellij.rt.coverage.instrumentation.filters.methods.MethodFilter;
import com.intellij.rt.coverage.instrumentation.kotlin.KotlinUtils;

import java.util.List;

public class FilterUtils {

  public static List<MethodFilter> createMethodFilters() {
    List<MethodFilter> result = KotlinUtils.createMethodFilters();
    result.add(new EnumMethodsFilter());
    result.add(new DeserializeLambdaFilter());
    return result;
  }

  public static List<ClassFilter> createClassFilters() {
    return KotlinUtils.createClassFilters();
  }

  public static List<LinesFilter> createLineFilters() {
    List<LinesFilter> result = KotlinUtils.createLineFilters();
    result.add(new ClosingBracesFilter());
    result.add(new AnnotationIgnoredMethodFilter());
    return result;
  }

  public static List<BranchesFilter> createBranchFilters() {
    List<BranchesFilter> result = KotlinUtils.createBranchFilters();
    result.add(new NotNullAssertionsFilter());
    return result;
  }
}
