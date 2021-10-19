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

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.SaveHook;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import jetbrains.coverage.report.ReportBuilderFactory;
import jetbrains.coverage.report.SourceCodeProvider;
import jetbrains.coverage.report.html.HTMLReportBuilder;
import jetbrains.coverage.report.idea.IDEACoverageData;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Reporter {
  private final List<BinaryReport> myReports;
  @Nullable
  private List<File> myOutputRoots;
  private ProjectData myProjectData;

  public Reporter(File dataFile, File sourceMapFile) {
    this(Collections.singletonList(new BinaryReport(dataFile, sourceMapFile)), null);
  }

  public Reporter(List<BinaryReport> reports, @Nullable List<File> outputRoots) {
    if (reports.isEmpty()) throw new RuntimeException("Report list is empty");
    myReports = reports;
    myOutputRoots = outputRoots;
  }

  public BinaryReport getReport() {
    return myReports.get(0);
  }

  private ProjectData getProjectData() throws IOException {
    if (myProjectData != null) return myProjectData;
    final ProjectData projectData = ProjectDataLoader.load(myReports.get(0).getDataFile());
    for (int i = 1; i < myReports.size(); i++) {
      projectData.merge(ProjectDataLoader.load(myReports.get(i).getDataFile()));
    }
    if (myOutputRoots != null) {
      final FileLocator fileLocator = new FileLocator(myOutputRoots);
      myOutputRoots = null;
      myProjectData = filterNonLocatableClasses(projectData, fileLocator);
      filterNonLocatableClasses(projectData, fileLocator);
    } else {
      myProjectData = projectData;
    }
    for (BinaryReport report : myReports) {
      SaveHook.loadAndApplySourceMap(myProjectData, report.getSourceMapFile());
    }
    return myProjectData;
  }

  private ProjectData filterNonLocatableClasses(ProjectData projectData, FileLocator fileLocator) {
    final ProjectData filteredData = new ProjectData();
    for (Map.Entry<String, ClassData> entry : projectData.getClasses().entrySet()) {
      if (fileLocator.locateClassFile(entry.getKey()).isEmpty()) continue;
      final ClassData classData = filteredData.getOrCreateClassData(entry.getKey());
      classData.setLines((LineData[]) entry.getValue().getLines());
    }
    return filteredData;
  }

  public void createXMLReport(File xmlFile) throws IOException {
    final XMLCoverageReport report = new XMLCoverageReport();
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(xmlFile);
      report.write(out, getProjectData());
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }

  public void createHTMLReport(File htmlDir, final List<File> sourceDirectories) throws IOException {
    final HTMLReportBuilder builder = ReportBuilderFactory.createHTMLReportBuilder();
    builder.setReportDir(htmlDir);
    final SourceCodeProvider sourceCodeProvider = new DirectorySourceCodeProvider(getProjectData(), sourceDirectories);
    builder.generateReport(new IDEACoverageData(getProjectData(), sourceCodeProvider));
  }
}
