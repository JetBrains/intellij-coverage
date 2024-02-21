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
  public static boolean FIELD_INSTRUMENTATION_ENABLED =
      "true".equals(System.getProperty("idea.new.sampling.coverage", "true"))
          && "true".equals(System.getProperty("idea.new.tracing.coverage", "true"));
  public static final boolean NEW_TEST_TRACKING_ENABLED = "true".equals(System.getProperty("idea.new.test.tracking.coverage", "true"));
  public static boolean CONDY_ENABLED = "true".equals(System.getProperty("coverage.condy.enable", "true"));
  public static final boolean INSTRUCTIONS_COVERAGE_ENABLED = "true".equals(System.getProperty("coverage.instructions.enable", "false"));
  public static boolean CALCULATE_HITS_COUNT = "true".equals(System.getProperty("idea.coverage.calculate.hits", "false"));
  public static boolean IGNORE_LOCAL_FUNCTIONS_IN_IGNORED_METHODS = "true".equals(System.getProperty("idea.coverage.ignore.local.functions.in.ignored.methods", "true"));
  public static final boolean TEST_MODE = "true".equals(System.getProperty("idea.coverage.test.mode", "false"));
  public static final boolean USE_SYSTEM_CLASS_LOADER = "true".equals(System.getProperty("idea.coverage.use.system.classloader", "false"));
}
