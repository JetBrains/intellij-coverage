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
    result.add(new ComposeSkippingBranchFilter());
    result.add(new ComposeTracingBranchFilter());
    result.add(new ComposeKeyCheckBranchFilter());
    result.add(new ComposeSourceInfoBranchFilter());
    result.add(new ComposeEndRestartGroupBranchFilter());
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
    return "Landroidx/compose/runtime/Composer;".equals(parameters[n - 2].getDescriptor())
        && "I".equals(parameters[n - 1].getDescriptor());
  }
}
