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

package com.intellij.rt.coverage.instrumentation.filters;

import com.intellij.rt.coverage.instrumentation.MethodFilteringVisitor;
import com.intellij.rt.coverage.instrumentation.filters.branches.*;
import com.intellij.rt.coverage.instrumentation.filters.classFilter.*;
import com.intellij.rt.coverage.instrumentation.filters.classes.*;
import com.intellij.rt.coverage.instrumentation.filters.lines.*;
import com.intellij.rt.coverage.instrumentation.filters.methods.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KotlinUtils {
  public static final String KOTLIN_DEFAULT_CONSTRUCTOR_MARKER = "Lkotlin/jvm/internal/DefaultConstructorMarker;";
  private static final String KOTLIN_CLASS_LABEL = "IS_KOTLIN";
  public static final String SEALED_CLASS_LABEL = "IS_SEALED_CLASS";
  public static final String KOTLIN_METADATA = "Lkotlin/Metadata;";

  public static boolean isKotlinClass(MethodFilteringVisitor context) {
    Object currentProperty = context.getProperty(KOTLIN_CLASS_LABEL);
    if (currentProperty instanceof Boolean) return (Boolean) currentProperty;
    boolean isKotlin = context.getAnnotations().contains(KOTLIN_METADATA);
    context.addProperty(KOTLIN_CLASS_LABEL, isKotlin);
    return isKotlin;
  }

  public static boolean isSealedClass(MethodFilteringVisitor context) {
    final Object currentProperty = context.getProperty(SEALED_CLASS_LABEL);
    if (currentProperty instanceof Boolean) return (Boolean) currentProperty;
    return false;
  }

  private static final boolean ourKotlinEnabled = !"false".equals(System.getProperty("coverage.kotlin.enable", "true"));

  public static List<MethodFilter> createMethodFilters() {
    if (!ourKotlinEnabled) return Collections.emptyList();
    List<MethodFilter> result = new ArrayList<MethodFilter>();
    result.add(new KotlinSyntheticConstructorOfSealedClassFilter());
    result.add(new KotlinSyntheticAccessMethodFilter());
    result.add(new KotlinLocalFunctionInsideIgnoredMethodFilter());
    return result;
  }

  public static List<ClassSignatureFilter> createClassSignatureFilters() {
    if (!ourKotlinEnabled) return Collections.emptyList();
    List<ClassSignatureFilter> result = new ArrayList<ClassSignatureFilter>();
    result.add(new KotlinFunctionOrPropertyReferenceFilter());
    result.add(new KotlinSerializerFilter());
    return result;
  }

  public static List<LinesFilter> createLineFilters() {
    if (!ourKotlinEnabled) return Collections.emptyList();
    List<LinesFilter> result = new ArrayList<LinesFilter>();
    result.add(new KotlinImplementerDefaultInterfaceMemberFilter());
    result.add(new KotlinCoroutinesLinesFilter());
    result.add(new KotlinInlineFilter());
    result.add(new KotlinDeprecatedMethodFilter());
    result.add(new KotlinDefaultArgsLineFilter());
    return result;
  }

  public static List<BranchesFilter> createBranchFilters() {
    if (!ourKotlinEnabled) return Collections.emptyList();
    List<BranchesFilter> result = new ArrayList<BranchesFilter>();
    result.add(new KotlinWhenMappingExceptionFilter());
    result.add(new KotlinDefaultArgsBranchFilter());
    result.add(new KotlinCoroutinesBranchesFilter());
    result.add(new KotlinLateinitFilter());
    result.add(new KotlinOpenMemberWithDefaultArgsFilter());
    result.add(new KotlinUnsafeCastFilter());
    result.add(new KotlinWhenStringFilter());
    return result;
  }

  public static List<ClassFilter> createClassFilters() {
    if (!ourKotlinEnabled) return Collections.emptyList();
    List<ClassFilter> result = new ArrayList<ClassFilter>();
    result.add(new KotlinValueClassFilter());
    return result;
  }
}
