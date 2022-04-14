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

package com.intellij.rt.coverage.util.classFinder;

import com.intellij.rt.coverage.util.ClassNameUtil;

import java.util.List;
import java.util.regex.Pattern;

public interface ClassFilter {
  boolean shouldInclude(String className);

  class PatternFilter implements ClassFilter {
    private final List<Pattern> myIncludePatterns;
    private final List<Pattern> myExcludePatterns;

    public PatternFilter(List<Pattern> includePatterns, List<Pattern> excludePatterns) {
      myIncludePatterns = includePatterns;
      myExcludePatterns = excludePatterns;
    }

    @Override
    public boolean shouldInclude(String className) {
      if (ClassNameUtil.matchesPatterns(className, myExcludePatterns)) return false;
      final String outerClassName = ClassNameUtil.getOuterClassName(className);
      if (ClassNameUtil.matchesPatterns(outerClassName, myIncludePatterns)) return true;
      return myIncludePatterns.isEmpty();
    }
  }
}
