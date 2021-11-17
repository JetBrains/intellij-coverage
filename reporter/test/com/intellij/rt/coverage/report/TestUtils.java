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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class TestUtils {
  @NotNull
  public static BinaryReport runTest(String patterns, String className) throws IOException, InterruptedException {
    final File icFile = File.createTempFile("report_tmp", ".ic");
    final File sourceMapFile = File.createTempFile("report_tmp", ".sm");
    patterns = "true " + sourceMapFile.getAbsolutePath() + " " + patterns;
    CoverageStatusTest.runCoverage(System.getProperty("java.class.path"), icFile, patterns, className, false);
    return new BinaryReport(icFile, sourceMapFile);
  }

  @NotNull
  public static File getResourceFile(String expectedFileName) {
    final String expectedPath = TestUtils.class.getClassLoader().getResource(expectedFileName).getPath();
    return new File(expectedPath);
  }

  public static Reporter createReporter(BinaryReport report, String output, String source) {
    final List<File> outputs = output == null ? null : Collections.singletonList(new File(output));
    final List<File> sources = source == null ? null : Collections.singletonList(new File(source));
    return new Reporter(Collections.singletonList(new Module(Collections.singletonList(report), outputs, sources)));
  }
}
