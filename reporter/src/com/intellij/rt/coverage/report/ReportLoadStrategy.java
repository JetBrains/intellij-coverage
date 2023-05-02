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
import com.intellij.rt.coverage.report.data.BinaryReport;
import com.intellij.rt.coverage.report.api.Filters;
import com.intellij.rt.coverage.report.data.Module;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class ReportLoadStrategy {
  protected final List<BinaryReport> myReports;
  protected final List<Module> myModules;
  private ProjectData myCacheData;

  protected ReportLoadStrategy(List<BinaryReport> reports, List<Module> modules) {
    myReports = reports;
    myModules = modules;
  }


  public ProjectData getProjectData() {
    if (myCacheData == null) {
      myCacheData = loadProjectData();
    }
    return myCacheData;
  }

  protected abstract ProjectData loadProjectData();


  public List<File> getSources() {
    final List<File> sources = new ArrayList<File>();
    for (Module module : myModules) {
      sources.addAll(module.getSources());
    }
    return sources;
  }

  /**
   * Default strategy. Collects merged coverage report from scratch via aggregator.
   */
  public static class RawReportLoadStrategy extends ReportLoadStrategy {
    private final Filters myFilters;

    public RawReportLoadStrategy(List<BinaryReport> reports, List<Module> modules, Filters filters) {
      super(reports, modules);
      myFilters = filters;
    }

    @Override
    protected ProjectData loadProjectData() {
      final Aggregator aggregator = new Aggregator(myReports, myModules, new Request(myFilters, null, null));
      return aggregator.getProjectData();
    }
  }

  /**
   * This strategy assumes that a single report file is passed, which is a result of previous
   * aggregator request.
   */
  public static class AggregatedReportLoadStrategy extends ReportLoadStrategy {

    protected AggregatedReportLoadStrategy(List<BinaryReport> reports, List<Module> modules) {
      super(reports, modules);
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
