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

package com.intellij.rt.coverage.aggregate.api;

import com.intellij.rt.coverage.aggregate.Aggregator;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.InstrumentationOptions;
import com.intellij.rt.coverage.report.data.BinaryReport;
import com.intellij.rt.coverage.util.CoverageReport;
import com.intellij.rt.coverage.util.ProjectDataLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AggregatorApi {
  private AggregatorApi() {
    // no-op
  }

  public static void aggregate(List<Request> requests, List<File> reports, List<File> outputRoots) {
    List<BinaryReport> binaryReports = new ArrayList<BinaryReport>();
    for (File report : reports) {
      binaryReports.add(new BinaryReport(report, null));
    }

    new Aggregator(binaryReports, outputRoots, requests).processRequests();
  }

  public static void merge(List<File> reports, File resultReport) {
    if (reports.isEmpty()) {
      // output file isn't created if no inputs
      return;
    }
    File firstFile = reports.get(0);

    ProjectData result = ProjectDataLoader.load(firstFile);
    for (int i = 1; i < reports.size(); i++) {
      ProjectData report = ProjectDataLoader.load(reports.get(i));
      result.merge(report);
    }

    CoverageReport.save(result, (new InstrumentationOptions.Builder()).setDataFile(resultReport).build());
  }
}
