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

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.util.CoverageIOUtil;
import jetbrains.coverage.report.ReportBuilderFactory;
import jetbrains.coverage.report.SourceCodeProvider;
import jetbrains.coverage.report.html.HTMLReportBuilder;
import jetbrains.coverage.report.idea.IDEACoverageData;
import jetbrains.coverage.report.impl.html.HTMLReportBuilderImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Creates XML or HTML coverage reports.
 */
public class Reporter {
  private final ReportLoadStrategy myLoad;

  public Reporter(ReportLoadStrategy loadStrategy) {
    myLoad = loadStrategy;
  }

  public void createXMLReport(File xmlFile) throws IOException {
    final XMLCoverageReport report = new XMLCoverageReport();
    FileOutputStream out = null;
    try {
      xmlFile.getParentFile().mkdirs();
      out = new FileOutputStream(xmlFile);
      report.write(out, myLoad.getProjectData());
    } finally {
      CoverageIOUtil.close(out);
    }
  }

  public void createHTMLReport(File htmlDir, String title, String charset) throws IOException {
    htmlDir.mkdirs();
    final HTMLReportBuilder builder = ReportBuilderFactory.createHTMLReportBuilderForKover();
    builder.setReportDir(htmlDir);
    if (builder instanceof HTMLReportBuilderImpl) {
      if (title != null) {
        ((HTMLReportBuilderImpl) builder).setReportTitle(title);
      }

      if (charset != null) {
        ((HTMLReportBuilderImpl) builder).setCharset(charset);
      }
    }
    final SourceCodeProvider sourceCodeProvider = new DirectorySourceCodeProvider(myLoad.getProjectData(), myLoad.getSources());
    builder.generateReport(new IDEACoverageData(myLoad.getProjectData(), sourceCodeProvider));
  }

  public ProjectData getProjectData() {
    return myLoad.getProjectData();
  }
}
