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

import com.intellij.rt.coverage.instrumentation.filters.branches.*;
import com.intellij.rt.coverage.instrumentation.filters.classFilter.*;
import com.intellij.rt.coverage.instrumentation.filters.classes.*;
import com.intellij.rt.coverage.instrumentation.filters.lines.*;
import com.intellij.rt.coverage.instrumentation.filters.methods.*;

import java.util.ArrayList;
import java.util.List;

public class FilterUtils {

  public static List<MethodFilter> createMethodFilters() {
    List<MethodFilter> result = KotlinUtils.createMethodFilters();
    result.add(new EnumMethodsFilter());
    result.add(new DeserializeLambdaFilter());
    return result;
  }

  public static List<ClassSignatureFilter> createClassSignatureFilters() {
    List<ClassSignatureFilter> result = new ArrayList<ClassSignatureFilter>();
    result.add(new ClassIgnoredByAnnotationFilter());
    result.addAll(KotlinUtils.createClassSignatureFilters());

    if (!(result.get(0) instanceof  ClassIgnoredByAnnotationFilter)) {
      throw new AssertionError("ClassIgnoredByAnnotationFilter filter should be the first one in the list of filters");
    }
    return result;
  }

  public static List<ClassFilter> createClassFilters() {
    List<ClassFilter> result = KotlinUtils.createClassFilters();
    result.add(new PrivateConstructorOfUtilClassFilter());
    result.add(new InheritanceCollectorFilter());
    return result;
  }

  public static List<CoverageFilter> createLineFilters() {
    List<CoverageFilter> result = KotlinUtils.createLineFilters();
//    result.add(new ClosingBracesFilter());
    result.add(new TryFinallyLineFilter());
    result.add(new TryWithResourcesLineFilter());
    result.add(new TryWithResourcesJava8LineFilter());
    result.add(new AnnotationIgnoredMethodFilter());

    if (!(result.get(result.size() - 1) instanceof  AnnotationIgnoredMethodFilter)) {
      throw new AssertionError("AnnotationIgnoredMethodFilter should be the last one in the list of filters");
    }
    return result;
  }

  public static List<CoverageFilter> createBranchFilters() {
    List<CoverageFilter> result = KotlinUtils.createBranchFilters();
    result.add(new NotNullAssertionsFilter());
    result.add(new AssertFilter());
    result.add(new BooleanOperatorFilter());
    result.add(new JavaStringSwitchFilter());
    return result;
  }
}
