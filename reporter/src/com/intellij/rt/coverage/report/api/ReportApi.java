/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

package com.intellij.rt.coverage.report.api;

import com.intellij.rt.coverage.report.ReportLoadStrategy;
import com.intellij.rt.coverage.report.Reporter;
import com.intellij.rt.coverage.report.data.BinaryReport;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReportApi {
  private ReportApi() {
    // no-op
  }

  public static void xmlReport(File xmlReportFile, List<File> reports, List<File> outputRoots, List<File> sourceRoots, Filters filters) throws IOException {
    Reporter reporter = createReporter(reports, outputRoots, sourceRoots, filters);
    reporter.createXMLReport(xmlReportFile);
  }

  public static void htmlReport(
      File htmlReportDir,
      @Nullable String title,
      @Nullable String charset,
      List<File> reports,
      List<File> outputRoots,
      List<File> sourceRoots,
      Filters filters
  ) throws IOException {
    Reporter reporter = createReporter(reports, outputRoots, sourceRoots, filters);
    reporter.createHTMLReport(htmlReportDir, title, charset);
  }

  private static Reporter createReporter(List<File> reports, List<File> outputRoots, List<File> sourceRoots, Filters filters) {
    List<BinaryReport> binaryReports = new ArrayList<BinaryReport>();
    for (File report : reports) {
      binaryReports.add(new BinaryReport(report, null));
    }

    ReportLoadStrategy loadStrategy =
        new ReportLoadStrategy.RawReportLoadStrategy(binaryReports, outputRoots, sourceRoots, filters);

    return new Reporter(loadStrategy);
  }
}
