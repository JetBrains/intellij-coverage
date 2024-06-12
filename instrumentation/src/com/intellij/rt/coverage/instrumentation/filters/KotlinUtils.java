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

import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.data.Key;
import com.intellij.rt.coverage.instrumentation.filters.branches.*;
import com.intellij.rt.coverage.instrumentation.filters.classFilter.*;
import com.intellij.rt.coverage.instrumentation.filters.classes.*;
import com.intellij.rt.coverage.instrumentation.filters.compose.ComposeUtils;
import com.intellij.rt.coverage.instrumentation.filters.lines.*;
import com.intellij.rt.coverage.instrumentation.filters.methods.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KotlinUtils {
  public static final String KOTLIN_DEFAULT_CONSTRUCTOR_MARKER = "Lkotlin/jvm/internal/DefaultConstructorMarker;";
  public static final String COMPANION_SUFFIX = "$Companion";

  public static boolean isKotlinClass(InstrumentationData data) {
    Boolean isKotlin = data.get(Key.IS_KOTLIN);
    return isKotlin != null && isKotlin;
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

  public static List<CoverageFilter> createLineFilters() {
    if (!ourKotlinEnabled) return Collections.emptyList();
    List<CoverageFilter> result = new ArrayList<CoverageFilter>();
    result.add(new KotlinImplementerDefaultInterfaceMemberFilter());
    result.add(new KotlinCoroutinesFilter());
    result.add(new KotlinDeprecatedMethodFilter());
    result.add(new KotlinDefaultArgsLineFilter());
    result.add(new KotlinTryFinallyLineFilter());

    result.addAll(ComposeUtils.createLineFilters());
    return result;
  }

  public static List<CoverageFilter> createBranchFilters() {
    if (!ourKotlinEnabled) return Collections.emptyList();
    List<CoverageFilter> result = new ArrayList<CoverageFilter>();
    result.add(new KotlinWhenMappingExceptionFilter());
    result.add(new KotlinDefaultArgsBranchFilter());
    result.add(new KotlinLateinitFilter());
    result.add(new KotlinOpenMemberWithDefaultArgsFilter());
    result.add(new KotlinUnsafeCastFilter());
    result.add(new KotlinWhenStringFilter());

    result.addAll(ComposeUtils.createBranchFilters());
    return result;
  }

  public static List<ClassFilter> createClassFilters() {
    if (!ourKotlinEnabled) return Collections.emptyList();
    List<ClassFilter> result = new ArrayList<ClassFilter>();
    result.add(new KotlinClassMarkerFilter());
    result.add(new KotlinValueClassFilter());
    return result;
  }
}
