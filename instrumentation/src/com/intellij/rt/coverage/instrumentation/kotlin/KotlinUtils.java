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

package com.intellij.rt.coverage.instrumentation.kotlin;

import com.intellij.rt.coverage.instrumentation.MethodFilteringVisitor;
import com.intellij.rt.coverage.instrumentation.filters.enumerating.*;
import com.intellij.rt.coverage.instrumentation.filters.signature.KotlinSyntheticAccessMethodFilter;
import com.intellij.rt.coverage.instrumentation.filters.signature.KotlinSyntheticConstructorOfSealedClassFilter;
import com.intellij.rt.coverage.instrumentation.filters.signature.MethodSignatureFilter;
import com.intellij.rt.coverage.instrumentation.filters.visiting.*;

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

  public static List<MethodSignatureFilter> createSignatureFilters() {
    if (!ourKotlinEnabled) return Collections.emptyList();
    List<MethodSignatureFilter> result = new ArrayList<MethodSignatureFilter>();
    result.add(new KotlinSyntheticConstructorOfSealedClassFilter());
    result.add(new KotlinSyntheticAccessMethodFilter());
    return result;
  }

  public static List<MethodVisitingFilter> createVisitingFilters() {
    if (!ourKotlinEnabled) return Collections.emptyList();
    List<MethodVisitingFilter> result = new ArrayList<MethodVisitingFilter>();
    result.add(new KotlinImplementerDefaultInterfaceMemberFilter());
    result.add(new KotlinCoroutinesVisitingFilter());
    result.add(new KotlinInlineVisitingFilter());
    result.add(new DeprecatedMethodFilter());
    result.add(new KotlinDefaultArgsLineFilter());
    return result;
  }

  public static List<LineEnumeratorFilter> createLineEnumeratorFilters() {
    if (!ourKotlinEnabled) return Collections.emptyList();
    List<LineEnumeratorFilter> result = new ArrayList<LineEnumeratorFilter>();
    result.add(new KotlinWhenMappingExceptionFilter());
    result.add(new KotlinDefaultArgsBranchFilter());
    result.add(new KotlinCoroutinesEnumeratingFilter());
    result.add(new KotlinLateinitFilter());
    result.add(new KotlinOpenMemberWithDefaultArgsFilter());
    result.add(new KotlinUnsafeCastFilter());
    return result;
  }
}
