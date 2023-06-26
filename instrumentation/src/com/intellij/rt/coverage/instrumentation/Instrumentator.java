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

package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.testTracking.TestTrackingArrayMode;
import com.intellij.rt.coverage.instrumentation.testTracking.TestTrackingClassDataMode;
import com.intellij.rt.coverage.instrumentation.testTracking.TestTrackingMode;
import com.intellij.rt.coverage.util.CoverageReport;
import com.intellij.rt.coverage.util.ErrorReporter;
import com.intellij.rt.coverage.util.OptionsUtil;
import com.intellij.rt.coverage.util.TestTrackingCallback;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This is an entry point for coverage agent. It accepts coverage parameters and enables classes transformation.
 */
public class Instrumentator {
  public static boolean ourIsInitialized = false;

  public static void premain(String argsString, Instrumentation instrumentation) throws Exception {
    new Instrumentator().performPremain(argsString, instrumentation);
  }

  public void performPremain(String argsString, Instrumentation instrumentation) throws Exception {
    checkLogLevel();

    synchronized (Instrumentator.class) {
      if (ourIsInitialized) {
        ErrorReporter.reportError("Coverage agent has been applied twice, ignore the second one.");
        return;
      }
      ourIsInitialized = true;
    }

    CoverageArgs args;
    try {
      args = CoverageArgs.fromString(argsString);
    } catch (IllegalArgumentException e) {
      ErrorReporter.reportError("Failed to parse agent arguments", e);
      System.exit(1);
      return;
    }

    ErrorReporter.logInfo("---- IntelliJ IDEA coverage runner ---- ");
    ErrorReporter.logInfo(args.branchCoverage ? ("Branch coverage " + (args.testTracking ? "with tracking per test coverage ..." : "...")) : "Line coverage ...");
    logPatterns(args.includePatterns, "include");
    logPatterns(args.excludePatterns, "exclude");
    logPatterns(args.annotationsToIgnore, "exclude annotations");

    final TestTrackingMode testTrackingMode = createTestTrackingMode(args.testTracking);
    final TestTrackingCallback callback = testTrackingMode == null ? null : testTrackingMode.createTestTrackingCallback();
    final ProjectData data = ProjectData.createProjectData(args.dataFile, null, args.testTracking, args.branchCoverage, args.includePatterns, args.excludePatterns, callback);
    data.setAnnotationsToIgnore(args.annotationsToIgnore);
    final ClassFinder cf = new ClassFinder(args.includePatterns, args.excludePatterns);

    final CoverageReport report = new CoverageReport(args.dataFile, args.calcUnloaded, cf, args.mergeData);
    report.setSourceMapFile(args.sourceMap);
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      public void run() {
        report.save(data);
      }
    }));

    final boolean shouldSaveSource = args.sourceMap != null;
    final CoverageTransformer transformer = new CoverageTransformer(data, shouldSaveSource, args.excludePatterns, args.includePatterns, cf, testTrackingMode);
    addTransformer(instrumentation, transformer);
  }

  private void logPatterns(List<Pattern> patterns, String name) {
    if (patterns.isEmpty()) return;
    ErrorReporter.logInfo(name + " patterns:");
    for (Pattern pattern : patterns) {
      ErrorReporter.logInfo(pattern.pattern());
    }
  }

  private void checkLogLevel() {
    if ("error".equals(OptionsUtil.LOG_LEVEL)) {
      ErrorReporter.setLogLevel(ErrorReporter.ERROR);
    } else {
      ErrorReporter.setLogLevel(ErrorReporter.INFO);
    }
  }

  /**
   * Add transformer with re-transformation enabled when possible.
   * Reflection is used for 1.5 compatibility.
   */
  private void addTransformer(Instrumentation instrumentation, CoverageTransformer transformer) {
    try {
      final Method method = Instrumentation.class.getMethod("addTransformer", ClassFileTransformer.class, boolean.class);
      method.invoke(instrumentation, transformer, true);
    } catch (NoSuchMethodException e) {
      instrumentation.addTransformer(transformer);
    } catch (Exception e) {
      ErrorReporter.reportError("Adding transformer failed.", e);
      System.exit(1);
    }
  }

  private TestTrackingMode createTestTrackingMode(boolean traceLines) {
    if (!traceLines) return null;
    if (OptionsUtil.NEW_BRANCH_COVERAGE_ENABLED && OptionsUtil.NEW_TEST_TRACKING_ENABLED) {
      return new TestTrackingArrayMode();
    }
    return new TestTrackingClassDataMode();
  }
}
