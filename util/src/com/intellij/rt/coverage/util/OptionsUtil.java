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

public class OptionsUtil {
  public static final boolean NEW_SAMPLING_ENABLED = System.getProperty("idea.new.sampling.coverage") != null;
  public static final boolean NEW_TRACING_ENABLED = System.getProperty("idea.new.tracing.coverage") != null;
  public static final boolean NEW_TEST_TRACKING_ENABLED = "true".equals(System.getProperty("idea.new.test.tracking.coverage", "true"));
  public static final boolean INSTRUCTIONS_COVERAGE_ENABLED = "true".equals(System.getProperty("coverage.instructions.enable", "false"));

  public static final boolean UNLOADED_CLASSES_FULL_ANALYSIS = "true".equals(System.getProperty("coverage.unloaded.classes.full.analysis", "true"));
  public static final boolean THREAD_SAFE_STORAGE = "true".equals(System.getProperty("idea.coverage.thread-safe.enabled", "true"));

  public static final boolean IGNORE_PRIVATE_CONSTRUCTOR_OF_UTIL_CLASS = "true".equals(System.getProperty("coverage.ignore.private.constructor.util.class", "false"));

  public static final String LOG_LEVEL = System.getProperty("idea.coverage.log.level");
}
