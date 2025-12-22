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

package com.intellij.rt.coverage.util;

import com.intellij.rt.coverage.instrumentation.CoverageRuntime;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@SuppressWarnings("unused")
public class IndyUtils {
  public static CallSite getHits(MethodHandles.Lookup lookup, String name, MethodType type, String className) {
    return constant(CoverageRuntime.getHits(className));
  }

  public static CallSite getHitsMask(MethodHandles.Lookup lookup, String name, MethodType type, String className) {
    return constant(CoverageRuntime.getHitsMask(className));
  }

  public static CallSite getTraceMask(MethodHandles.Lookup lookup, String name, MethodType type, String className) {
    return constant(CoverageRuntime.getTraceMask(className));
  }

  public static CallSite loadClassData(MethodHandles.Lookup lookup, String name, MethodType type, String className) {
    return constant(CoverageRuntime.loadClassData(className));
  }
  
  private static CallSite constant(Object cst) {
    return new ConstantCallSite(MethodHandles.constant(Object.class, cst));
  }
}
