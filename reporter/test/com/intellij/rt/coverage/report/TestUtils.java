/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package com.intellij.rt.coverage.report;

import com.intellij.rt.coverage.CoverageStatusTest;
import com.intellij.rt.coverage.aggregate.Aggregator;
import com.intellij.rt.coverage.report.data.BinaryReport;
import com.intellij.rt.coverage.report.data.Filters;
import com.intellij.rt.coverage.report.data.Module;
import com.intellij.rt.coverage.report.util.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class TestUtils {
  public static final String JAVA_OUTPUT = join("build", "classes", "java", "test");
  public static final String KOTLIN_OUTPUT = join("build", "classes", "kotlin", "test");

  public static File createTmpFile() throws IOException {
    final Path directory = Files.createTempDirectory("coverage");
    return Files.createTempFile(directory, "test", ".ic").toFile();
  }

  @NotNull
  public static BinaryReport runTest(String patterns, String className) throws IOException, InterruptedException {
    final File icFile = createTmpFile();
    CoverageStatusTest.runCoverage(System.getProperty("java.class.path"), icFile, patterns, className, false, new String[]{"-Dcoverage.ignore.private.constructor.util.class=true"}, false, false);
    checkLogFile(icFile.getParentFile());
    return new BinaryReport(icFile, null);
  }

  public static String clearLogFile(File directory) throws IOException {
    final File logFile = new File(directory, "coverage-error.log");
    if (logFile.exists()) {
      final String content = FileUtils.readAll(logFile);
      logFile.delete();
      return content;
    }
    return null;
  }

  public static void checkLogFile(File directory) throws IOException {
    final String content = clearLogFile(directory);
    if (content != null && !content.isEmpty()) {
      throw new RuntimeException("Log file is not empty!\n" + content);
    }
  }

  @NotNull
  public static File getResourceFile(String expectedFileName) {
    final String expectedPath = TestUtils.class.getClassLoader().getResource(expectedFileName).getPath();
    return new File(expectedPath);
  }

  public static Reporter createRawReporter(BinaryReport report, String patterns) {
    final List<Module> modules = getModules();
    final List<BinaryReport> reports = report == null ? Collections.<BinaryReport>emptyList() : Collections.singletonList(report);
    final Filters filters = getFilters(patterns);
    return new Reporter(new ReportLoadStrategy.RawReportLoadStrategy(reports, modules, filters));
  }

  public static Reporter createReporter(BinaryReport report, String patterns) {
    final File smapFile = new File(report.getDataFile().getAbsolutePath() + ".sm");
    final BinaryReport aggregatedReport = new BinaryReport(report.getDataFile(), smapFile);
    runAggregator(aggregatedReport, patterns);

    final List<BinaryReport> reports = Collections.singletonList(aggregatedReport);
    final List<Module> modules = getModules();
    return new Reporter(new ReportLoadStrategy.AggregatedReportLoadStrategy(reports, modules));
  }

  public static void runAggregator(BinaryReport report, String patterns) {
    final Filters filters = getFilters(patterns);
    final Aggregator.Request request = new Aggregator.Request(filters, report.getDataFile(), report.getSourceMapFile());
    new Aggregator(Collections.singletonList(report), getModules(), request).processRequests();
  }

  @NotNull
  private static Filters getFilters(String patterns) {
    final List<Pattern> includes = new ArrayList<Pattern>();
    final List<Pattern> excludes = new ArrayList<Pattern>();
    final List<Pattern> excludeAnnotations = new ArrayList<Pattern>();
    final List<Pattern>[] lists = new List[]{includes, excludes, excludeAnnotations};
    int state = 0;
    for (String pattern : patterns.split(" ")) {
      if (pattern.isEmpty()) continue;
      if (pattern.equals("-exclude")) {
        state = 1;
        continue;
      }
      if (pattern.equals("-excludeAnnotations")) {
        state = 2;
        continue;
      }
      lists[state].add(Pattern.compile(pattern));
    }
    return new Filters(includes, excludes, excludeAnnotations);
  }

  public static List<Module> getModules() {
    final List<File> output = new ArrayList<File>();
    output.add(new File(KOTLIN_OUTPUT));
    output.add(new File(JAVA_OUTPUT));
    return Collections.singletonList(new Module(output, getSources()));
  }

  @NotNull
  private static List<File> getSources() {
    return Collections.singletonList(new File("test"));
  }

  public static String join(String first, String... other) {
    return Paths.get(first, other).toFile().getPath();
  }
}
