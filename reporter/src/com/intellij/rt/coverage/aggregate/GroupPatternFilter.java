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

package com.intellij.rt.coverage.aggregate;

import com.intellij.rt.coverage.util.classFinder.ClassFilter;

import java.util.List;

public class GroupPatternFilter implements ClassFilter {
  private final List<PatternFilter> myInternalFilters;

  public GroupPatternFilter(List<PatternFilter> filters) {
    myInternalFilters = filters;
  }

  @Override
  public boolean shouldInclude(String className) {
    for (PatternFilter filter : myInternalFilters) {
      if (filter.shouldInclude(className)) return true;
    }
    return false;
  }
}
