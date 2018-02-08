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

public class StringUtil {
  public static String join(String separator, String... values) {
    if (values.length == 0) return "";
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < values.length; i++) {
      String value = values[i];
      sb.append(value);
      if (i != values.length - 1) sb.append(separator);
    }
    return sb.toString();
  }
}
