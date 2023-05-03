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

package com.intellij.rt.coverage.report;

import com.intellij.rt.coverage.aggregate.Aggregator;
import com.intellij.rt.coverage.aggregate.api.Request;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.report.api.Filters;
import com.intellij.rt.coverage.report.data.BinaryReport;

import java.io.File;
import java.util.List;

public abstract class ReportLoadStrategy {
  protected final List<BinaryReport> myReports;
  protected final List<File> myOutputRoots;
  protected final List<File> mySourceRoots;
  private ProjectData myCacheData;

  protected ReportLoadStrategy(List<BinaryReport> reports, List<File> outputRoots, List<File> sourceRoots) {
    myReports = reports;
    myOutputRoots = outputRoots;
    mySourceRoots = sourceRoots;
  }


  public ProjectData getProjectData() {
    if (myCacheData == null) {
      myCacheData = loadProjectData();
    }
    return myCacheData;
  }

  protected abstract ProjectData loadProjectData();


  public List<File> getSources() {
    return mySourceRoots;
  }

  /**
   * Default strategy. Collects merged coverage report from scratch via aggregator.
   */
  public static class RawReportLoadStrategy extends ReportLoadStrategy {
    private final Filters myFilters;

    public RawReportLoadStrategy(List<BinaryReport> reports, List<File> outputRoots, List<File> sourceRoots, Filters filters) {
      super(reports, outputRoots, sourceRoots);
      myFilters = filters;
    }

    @Override
    protected ProjectData loadProjectData() {
      final Aggregator aggregator = new Aggregator(myReports, myOutputRoots, new Request(myFilters, null, null));
      return aggregator.getProjectData();
    }
  }

  /**
   * This strategy assumes that a single report file is passed, which is a result of previous
   * aggregator request.
   */
  public static class AggregatedReportLoadStrategy extends ReportLoadStrategy {

    protected AggregatedReportLoadStrategy(List<BinaryReport> reports, List<File> outputRoots, List<File> sourceRoots) {
      super(reports, outputRoots, sourceRoots);
      if (reports.size() != 1) {
        throw new IllegalArgumentException("One aggregated report expected, but " + reports.size() + " reports found.");
      }
    }

    @Override
    protected ProjectData loadProjectData() {
      return myReports.get(0).loadData();
    }
  }
}
