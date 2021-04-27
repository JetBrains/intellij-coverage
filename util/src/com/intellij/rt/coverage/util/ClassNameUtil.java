/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package com.intellij.rt.coverage.util;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Pavel.Sher
 */
public class ClassNameUtil {
  public static String getOuterClassName(String className) {
    int idx = className.indexOf('$');
    if (idx == -1) return className;
    return className.substring(0, idx);
  }

  public static boolean matchesPatterns(String className, List<Pattern> patterns) {
    for (Pattern excludePattern : patterns) {
      if (excludePattern.matcher(className).matches()) return true;
    }
    return false;
  }

  public static String convertToFQName(String className) {
    return className.replace('\\', '.').replace('/', '.');
  }
}
