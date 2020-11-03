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

import com.intellij.rt.coverage.instrumentation.Instrumenter;

public class KotlinUtils {
  private static final String KOTLIN_CLASS_LABEL = "IS_KOTLIN";

  public static boolean isKotlinClass(Instrumenter context) {
    Object currentProperty = context.getProperty(KOTLIN_CLASS_LABEL);
    if (currentProperty instanceof Boolean) return (Boolean) currentProperty;
    boolean isKotlin = context.getAnnotations().contains("Lkotlin/Metadata;");
    context.addProperty(KOTLIN_CLASS_LABEL, isKotlin);
    return isKotlin;
  }
}
