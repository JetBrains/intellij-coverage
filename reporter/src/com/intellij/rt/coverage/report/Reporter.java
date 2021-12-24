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

import com.intellij.rt.coverage.report.data.ProjectReport;
import jetbrains.coverage.report.ReportBuilderFactory;
import jetbrains.coverage.report.SourceCodeProvider;
import jetbrains.coverage.report.html.HTMLReportBuilder;
import jetbrains.coverage.report.idea.IDEACoverageData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Reporter {
  private final ProjectReport myProjectReport;

  public Reporter(ProjectReport projectReport) {
    myProjectReport = projectReport;
  }

  public void createXMLReport(File xmlFile) throws IOException {
    final XMLCoverageReport report = new XMLCoverageReport();
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(xmlFile);
      report.write(out, myProjectReport.getProjectData());
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }

  public void createHTMLReport(File htmlDir) throws IOException {
    final HTMLReportBuilder builder = ReportBuilderFactory.createHTMLReportBuilder();
    builder.setReportDir(htmlDir);
    final SourceCodeProvider sourceCodeProvider = new DirectorySourceCodeProvider(myProjectReport.getProjectData(), myProjectReport.getSources());
    builder.generateReport(new IDEACoverageData(myProjectReport.getProjectData(), sourceCodeProvider));
  }
}
