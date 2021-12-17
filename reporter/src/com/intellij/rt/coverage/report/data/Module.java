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

package com.intellij.rt.coverage.report.data;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.SaveHook;
import com.intellij.rt.coverage.report.util.ClassFileLocator;
import com.intellij.rt.coverage.report.util.FileLocator;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;
import com.intellij.rt.coverage.util.classFinder.ClassPathEntry;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.regex.Pattern;

public class Module {

  private final List<BinaryReport> myReports;
  private final List<File> myOutputRoots;
  private final List<File> mySourceRoots;
  private ProjectData myProjectData;

  public Module(List<BinaryReport> reports, List<File> outputRoots, List<File> sourceRoots) {
    myReports = reports;
    myOutputRoots = outputRoots;
    mySourceRoots = sourceRoots;
  }


  public ProjectData getProjectData() throws IOException {
    if (myProjectData != null) return myProjectData;
    if (myReports.isEmpty()) {
      myProjectData = collectCoverageInformationFromOutputs();
      return myProjectData;
    }
    final ProjectData projectData = ProjectDataLoader.load(myReports.get(0).getDataFile());
    for (int i = 1; i < myReports.size(); i++) {
      projectData.merge(ProjectDataLoader.load(myReports.get(i).getDataFile()));
    }
    if (myOutputRoots != null) {
      final FileLocator fileLocator = new ClassFileLocator(myOutputRoots);
      myProjectData = filterNonLocatableClasses(projectData, fileLocator);
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
      if (fileLocator.locate(entry.getKey()).isEmpty()) continue;
      final ClassData classData = filteredData.getOrCreateClassData(entry.getKey());
      classData.setLines((LineData[]) entry.getValue().getLines());
    }
    return filteredData;
  }

  private ProjectData collectCoverageInformationFromOutputs() {
    final ProjectData projectData = new ProjectData();
    if (myOutputRoots == null) return projectData;
    SaveHook.appendUnloadedFullAnalysis(projectData, new OutputClassFinder(), true, false);
    projectData.checkLineMappings();
    return projectData;
  }

  public List<File> getSources() {
    if (mySourceRoots == null) return Collections.emptyList();
    return mySourceRoots;
  }

  public List<BinaryReport> getReports() {
    return myReports;
  }

  public List<File> getOutputRoots() {
    return myOutputRoots;
  }


  private class OutputClassFinder extends ClassFinder {
    public OutputClassFinder() {
      super(Collections.<Pattern>emptyList(), Collections.<Pattern>emptyList());
    }

    @Override
    protected Collection<ClassPathEntry> getClassPathEntries() {
      final List<ClassPathEntry> entries = new ArrayList<ClassPathEntry>();
      for (File outputRoot : myOutputRoots) {
        entries.add(new ClassPathEntry(outputRoot.getAbsolutePath()));
      }
      return entries;
    }
  }
}
