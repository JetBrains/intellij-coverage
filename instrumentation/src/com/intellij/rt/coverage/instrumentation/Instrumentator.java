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
import com.intellij.rt.coverage.instrumentation.data.ProjectContext;
import com.intellij.rt.coverage.instrumentation.testTracking.TestTrackingArrayMode;
import com.intellij.rt.coverage.instrumentation.testTracking.TestTrackingClassDataMode;
import com.intellij.rt.coverage.instrumentation.testTracking.TestTrackingMode;
import com.intellij.rt.coverage.util.CoverageReport;
import com.intellij.rt.coverage.util.ErrorReporter;
import com.intellij.rt.coverage.util.OptionsUtil;
import com.intellij.rt.coverage.util.TestTrackingCallback;

import java.io.File;
import java.io.IOException;
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
    synchronized (Instrumentator.class) {
      if (ourIsInitialized) {
        ErrorReporter.info("Coverage agent has been applied twice, ignore the second one.");
        return;
      }
      ourIsInitialized = true;
    }

    CoverageArgs args;
    try {
      args = CoverageArgs.fromString(argsString);
    } catch (IllegalArgumentException e) {
      ErrorReporter.error("Failed to parse agent arguments", e);
      System.exit(1);
      return;
    }

    ErrorReporter.printInfo("---- IntelliJ IDEA coverage runner ---- ");
    ErrorReporter.printInfo((args.branchCoverage ? "Branch coverage " : "Line coverage ") + (args.testTracking ? "with tracking per test coverage ..." : "..."));
    logPatterns(args.includePatterns, "include");
    logPatterns(args.excludePatterns, "exclude");
    logPatterns(args.annotationsToIgnore, "exclude annotations");

    final TestTrackingMode testTrackingMode = createTestTrackingMode(args.testTracking);
    final TestTrackingCallback callback = testTrackingMode == null ? null : testTrackingMode.createTestTrackingCallback(args.dataFile);

    final InstrumentationOptions options = new InstrumentationOptions.Builder()
        .setBranchCoverage(args.branchCoverage)
        .setIsMergeData(args.mergeData)
        .setIsCalculateUnloaded(args.calcUnloaded)
        .setInstructionCoverage(OptionsUtil.INSTRUCTIONS_COVERAGE_ENABLED)
        .setIsCalculateHits(OptionsUtil.CALCULATE_HITS_COUNT)
        .setIncludePatterns(args.includePatterns)
        .setExcludePatterns(args.excludePatterns)
        .setExcludeAnnotations(args.annotationsToIgnore)
        .setDataFile(args.dataFile)
        .setSourceMapFile(args.sourceMap)
        .setTestTrackingMode(testTrackingMode)
        .build();

    createDataFile(args.dataFile);
    final ProjectData data = new ProjectData(callback);
    CoverageRuntime.installRuntime(data);

    final ProjectContext instrumentationData = new ProjectContext(options);
    final CoverageTransformer transformer = new CoverageTransformer(data, instrumentationData);
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      public void run() {
        transformer.stop();
        CoverageReport.save(data, instrumentationData);
      }
    }));

    addTransformer(instrumentation, transformer);
  }

  private void createDataFile(File dataFile) throws IOException {
    if (dataFile != null && !dataFile.exists()) {
      final File parentDir = dataFile.getParentFile();
      if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();
      dataFile.createNewFile();
    }
  }

  private void logPatterns(List<Pattern> patterns, String name) {
    if (patterns.isEmpty()) return;
    ErrorReporter.printInfo(name + " patterns:");
    for (Pattern pattern : patterns) {
      ErrorReporter.printInfo(pattern.pattern());
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
      ErrorReporter.error("Adding transformer failed.", e);
      System.exit(1);
    }
  }

  private TestTrackingMode createTestTrackingMode(boolean traceLines) {
    if (!traceLines) return null;
    return OptionsUtil.NEW_TEST_TRACKING_ENABLED ? new TestTrackingArrayMode() : new TestTrackingClassDataMode();
  }
}
