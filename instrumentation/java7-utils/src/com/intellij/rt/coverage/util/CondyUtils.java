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

package com.intellij.rt.coverage.util;

import com.intellij.rt.coverage.instrumentation.CoverageRuntime;

import java.lang.invoke.MethodHandles;

@SuppressWarnings("unused")
public class CondyUtils {
  public static int[] getHitsMask(MethodHandles.Lookup lookup, String name, Class<?> clazz, String className) {
    return CoverageRuntime.getHitsMask(className);
  }

  public static boolean[] getTraceMask(MethodHandles.Lookup lookup, String name, Class<?> clazz, String className) {
    return CoverageRuntime.getTraceMask(className);
  }

  public static Object loadClassData(MethodHandles.Lookup lookup, String name, Class<?> clazz, String className) {
    return CoverageRuntime.loadClassData(className);
  }
}
