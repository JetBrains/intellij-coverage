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
import com.intellij.rt.coverage.report.data.BinaryReport;
import com.intellij.rt.coverage.report.data.Module;
import com.intellij.rt.coverage.report.data.ProjectReport;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class TestUtils {
  @NotNull
  public static BinaryReport runTest(String patterns, String className) throws IOException, InterruptedException {
    final File icFile = File.createTempFile("report_tmp", ".ic");
    final File sourceMapFile = File.createTempFile("report_tmp", ".sm");
    patterns = "true " + sourceMapFile.getAbsolutePath() + " " + patterns;
    CoverageStatusTest.runCoverage(System.getProperty("java.class.path"), icFile, patterns, className, false, new String[]{"-Dcoverage.instructions.enable=true", "-Dcoverage.ignore.private.constructor.util.class=true"}, false, false);
    return new BinaryReport(icFile, sourceMapFile);
  }

  @NotNull
  public static File getResourceFile(String expectedFileName) {
    final String expectedPath = TestUtils.class.getClassLoader().getResource(expectedFileName).getPath();
    return new File(expectedPath);
  }

  public static Reporter createReporter(BinaryReport report, String patterns) {
    final String kotlinOutput = "build" + File.separator + "classes" + File.separator + "kotlin" + File.separator + "test";
    final String javaOutput = "build" + File.separator + "classes" + File.separator + "java" + File.separator + "test";
    final List<File> output = new ArrayList<File>();
    output.add(new File(kotlinOutput));
    output.add(new File(javaOutput));
    final List<BinaryReport> reports = report == null ? Collections.<BinaryReport>emptyList() : Collections.singletonList(report);
    final List<Pattern> includes = new ArrayList<Pattern>();
    final List<Pattern> excludes = new ArrayList<Pattern>();
    boolean isInclude = true;
    for (String pattern : patterns.split(" ")) {
      if (pattern.isEmpty()) continue;
      if (pattern.equals("-exlude")) {
        isInclude = false;
        continue;
      }
      (isInclude ? includes : excludes).add(Pattern.compile(pattern));
    }
    return new Reporter(new ProjectReport(reports, Collections.singletonList(new Module(output, getSources())), includes, excludes));
  }

  @NotNull
  private static List<File> getSources() {
    return Collections.singletonList(new File("test"));
  }
}
