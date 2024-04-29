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

package com.intellij.rt.coverage.aggregate;

import com.intellij.rt.coverage.util.ClassNameUtil;

import java.util.*;
import java.util.regex.Pattern;

public class InheritanceFilter {
  private final Map<String, IncludeStatus> myStatus = new HashMap<String, IncludeStatus>();
  private final Map<String, String[]> myInherits;

  public InheritanceFilter(Map<String, String[]> inherits) {
    myInherits = inherits;
  }

  public List<String> filterInherits(Collection<String> classes, List<Pattern> includePatterns, List<Pattern> excludePatterns) {
    List<String> result = new ArrayList<String>();
    myStatus.clear();
    for (String className : classes) {
      IncludeStatus status = isIncluded(className, includePatterns, excludePatterns);
      if (status == IncludeStatus.INCLUDED
          || status == IncludeStatus.UNKNOWN && includePatterns.isEmpty()) {
        result.add(className);
      }
    }
    myStatus.clear();
    return result;
  }

  private IncludeStatus isIncluded(String className, List<Pattern> includePatterns, List<Pattern> excludePatterns) {
    IncludeStatus status = myStatus.get(className);
    if (status != null) return status;

    status = isIncludedInternal(className, includePatterns, excludePatterns);
    myStatus.put(className, status);
    return status;
  }

  private IncludeStatus isIncludedInternal(String className, List<Pattern> includePatterns, List<Pattern> excludePatterns) {
    if (ClassNameUtil.matchesPatterns(className, excludePatterns)) return IncludeStatus.EXCLUDED;
    if (ClassNameUtil.matchesPatterns(className, includePatterns)) return IncludeStatus.INCLUDED_SELF;

    String[] inherits = myInherits.get(className);
    IncludeStatus status = IncludeStatus.UNKNOWN;
    if (inherits != null) {
      for (String inherit : inherits) {
        IncludeStatus inheritStatus = isIncluded(inherit, includePatterns, excludePatterns);
        if (inheritStatus == IncludeStatus.EXCLUDED) return IncludeStatus.EXCLUDED;
        if (inheritStatus == IncludeStatus.INCLUDED || inheritStatus == IncludeStatus.INCLUDED_SELF) {
          status = IncludeStatus.INCLUDED;
        }
      }
    }

    return status;
  }

  enum IncludeStatus {
    INCLUDED,
    // Do not include classes matched by filter, only their inheritants
    INCLUDED_SELF,
    EXCLUDED,
    UNKNOWN
  }
}
