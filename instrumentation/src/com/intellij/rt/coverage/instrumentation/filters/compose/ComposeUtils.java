/*
 * Copyright 2000-2024 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation.filters.compose;

import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.filters.KotlinUtils;
import com.intellij.rt.coverage.instrumentation.filters.lines.CoverageFilter;
import org.jetbrains.coverage.org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

public class ComposeUtils {
  public static List<CoverageFilter> createBranchFilters() {
    List<CoverageFilter> result = new ArrayList<CoverageFilter>();
    result.add(new ComposeCheckingBranchFilter());
    result.add(new ComposeKeyCheckBranchFilter());
    return result;
  }

  public static List<CoverageFilter> createLineFilters() {
    List<CoverageFilter> result = new ArrayList<CoverageFilter>();
    result.add(new ComposeSkipGroupLineFilter());
    result.add(new ComposeTraceEventStartLineFilter());
    return result;
  }

  static boolean isComposeMethod(InstrumentationData data) {
    if (!KotlinUtils.isKotlinClass(data)) return false;
    String desc = data.getMethodDesc();
    return isComposeMethod(desc);
  }

  static boolean isComposeMethod(String desc) {
    Type type = Type.getType(desc);
    Type[] parameters = type.getArgumentTypes();
    int n = parameters.length;

    if (n < 2) return false;
    int composerIndex = getComposerIndex(parameters);
    return composerIndex != -1;
  }

  static int getComposerIndex(Type[] parameters) {
    int i = parameters.length - 1;
    if (i < 0) return -1;
    while (i >= 0 && "I".equals(parameters[i].getDescriptor())) {
      i--;
    }
    if (i < 0 || i == parameters.length - 1) return -1;
    return "Landroidx/compose/runtime/Composer;".equals(parameters[i].getDescriptor()) ? i : -1;
  }
}
