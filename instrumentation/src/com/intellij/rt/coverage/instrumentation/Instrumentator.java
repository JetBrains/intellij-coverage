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

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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

    String[] args;
    if (argsString != null) {
      File argsFile = new File(argsString);
      if (argsFile.isFile()) {
        try {
          args = readArgsFromFile(argsString);
        } catch (IOException e) {
          ErrorReporter.reportError("Arguments were not passed correctly", e);
          return;
        }
      } else {
        args = tokenize(argsString);
      }
    } else {
      ErrorReporter.reportError("Argument string should be passed");
      return;
    }

    if (args.length < 5) {
      ErrorReporter.logError("At least 5 arguments expected but " + args.length + " found.\n"
          + '\'' + argsString + "'\n"
          + "Expected arguments are:\n"
          + "1) data file to save coverage result\n"
          + "2) a flag to enable tracking per test coverage\n"
          + "3) a flag to calculate coverage for unloaded classes\n"
          + "4) a flag to use data file as initial coverage, also use it if several parallel processes are to write into one file\n"
          + "5) a flag to run line coverage or branch coverage otherwise\n");
      System.exit(1);
    }

    final File dataFile = new File(args[0]);
    final boolean testTracking = Boolean.parseBoolean(args[1]);
    final boolean calcUnloaded = Boolean.parseBoolean(args[2]);
    final boolean mergeData = Boolean.parseBoolean(args[3]);
    final boolean branchCoverage = !Boolean.parseBoolean(args[4]);
    ErrorReporter.setBasePath(dataFile.getParent());

    int i = 5;
    final File sourceMapFile;
    if (args.length > 5 && Boolean.parseBoolean(args[5])) {
      sourceMapFile = new File(args[6]);
      i = 7;
    } else {
      sourceMapFile = null;
    }

    ErrorReporter.logInfo("---- IntelliJ IDEA coverage runner ---- ");
    ErrorReporter.logInfo(branchCoverage ? ("Branch coverage " + (testTracking ? "with tracking per test coverage ..." : "...")) : "Line coverage ...");

    final List<Pattern> includePatterns = new ArrayList<Pattern>();
    i = readPatterns(includePatterns, i, args, "include");

    final List<Pattern> excludePatterns = new ArrayList<Pattern>();
    if (i < args.length && "-exclude".equals(args[i])) {
      i = readPatterns(excludePatterns, i + 1, args, "exclude");
    }

    final List<Pattern> annotationsToIgnore = new ArrayList<Pattern>();
    if (i < args.length && "-excludeAnnotations".equals(args[i])) {
      readPatterns(annotationsToIgnore, i + 1, args, "exclude annotations");
    }

    final TestTrackingMode testTrackingMode = createTestTrackingMode(testTracking);
    final TestTrackingCallback callback = testTrackingMode == null ? null : testTrackingMode.createTestTrackingCallback();
    final ProjectData data = ProjectData.createProjectData(dataFile, null, testTracking, branchCoverage, includePatterns, excludePatterns, callback);
    data.setAnnotationsToIgnore(annotationsToIgnore);
    final ClassFinder cf = new ClassFinder(includePatterns, excludePatterns);

    final CoverageReport report = new CoverageReport(dataFile, calcUnloaded, cf, mergeData);
    report.setSourceMapFile(sourceMapFile);
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      public void run() {
        report.save(data);
      }
    }));

    final boolean shouldSaveSource = sourceMapFile != null;
    final CoverageTransformer transformer = new CoverageTransformer(data, shouldSaveSource, excludePatterns, includePatterns, cf, testTrackingMode);
    addTransformer(instrumentation, transformer);
  }

  private int readPatterns(final List<Pattern> patterns, int i, final String[] args, final String name) {
    ErrorReporter.logInfo(name + " patterns:");
    for (; i < args.length; i++) {
      if (args[i].startsWith("-")) break;
      try {
        patterns.add(Pattern.compile(args[i]));
        ErrorReporter.logInfo(args[i]);
      } catch (PatternSyntaxException ex) {
        ErrorReporter.reportError("Problem occurred with " + name + " pattern " + args[i] +
            ". This may cause no tests run and no coverage collected", ex);
        System.exit(1);
      }
    }
    return i;
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

  private String[] readArgsFromFile(String arg) throws IOException {
    final List<String> result = new ArrayList<String>();
    final File file = new File(arg);
    final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
    try {
      while (reader.ready()) {
        result.add(reader.readLine());
      }
    } finally {
      reader.close();
    }
    return result.toArray(new String[0]);
  }

  private static String[] tokenize(String argumentString) {
    List<String> tokenizedArgs = new ArrayList<String>();
    StringBuilder currentArg = new StringBuilder();
    for (int i = 0; i < argumentString.length(); i++) {
      char c = argumentString.charAt(i);
      switch (c) {
        default:
          currentArg.append(c);
          break;
        case ' ':
          String arg = currentArg.toString();
          if (arg.length() > 0) {
            tokenizedArgs.add(arg);
          }
          currentArg = new StringBuilder();
          break;
        case '\"':
          for (i++; i < argumentString.length(); i++) {
            char d = argumentString.charAt(i);
            if (d == '\"') {
              break;
            }
            currentArg.append(d);
          }
      }
    }

    String arg = currentArg.toString();
    if (arg.length() > 0) {
      tokenizedArgs.add(arg);
    }
    return tokenizedArgs.toArray(new String[0]);
  }
}
