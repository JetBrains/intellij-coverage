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
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.SaveHook;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;
import com.intellij.rt.coverage.util.classFinder.ClassPathEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

public class ProjectReport {
  private final List<BinaryReport> myReports;
  private final List<Module> myModules;
  private final List<Pattern> myIncludeClassPatterns;
  private final List<Pattern> myExcludeClassPatterns;
  private ProjectData myProjectData;

  public ProjectReport(List<BinaryReport> reports, List<Module> modules, List<Pattern> includeClassPatterns, List<Pattern> excludeClassPatterns) {
    myReports = reports;
    myModules = modules;
    myIncludeClassPatterns = includeClassPatterns;
    myExcludeClassPatterns = excludeClassPatterns;
  }

  public ProjectData getProjectData() {
    if (myProjectData != null) return myProjectData;
    final ProjectData projectData = collectCoverageInformationFromOutputs();
    for (BinaryReport report : myReports) {
      final ProjectData data = ProjectDataLoader.load(report.getDataFile());

      for (ClassData classData : data.getClasses().values()) {
        final ClassData collectedClassData = projectData.getClassData(classData.getName());
        if (collectedClassData == null) {
          // projectData contains all classes already filtered by outputs and filters
          // so this class must be filtered
          continue;
        }
        collectedClassData.merge(classData);
      }

    }
    myProjectData = projectData;
    return projectData;
  }

  private ProjectData collectCoverageInformationFromOutputs() {
    final ProjectData projectData = new ProjectData();
    SaveHook.appendUnloadedFullAnalysis(projectData, new OutputClassFinder(), true, false);
    return projectData;
  }

  public List<File> getSources() {
    final List<File> sources = new ArrayList<File>();
    for (Module module : myModules) {
      sources.addAll(module.getSources());
    }
    return sources;
  }

  private class OutputClassFinder extends ClassFinder {
    public OutputClassFinder() {
      super(myIncludeClassPatterns, myExcludeClassPatterns);
    }

    @Override
    protected Collection<ClassPathEntry> getClassPathEntries() {
      final List<ClassPathEntry> entries = new ArrayList<ClassPathEntry>();
      for (Module module : myModules) {
        List<File> outputs = module.getOutputRoots();
        if (outputs == null) continue;
        for (File outputRoot : outputs) {
          entries.add(new ClassPathEntry(outputRoot.getAbsolutePath()));
        }
      }
      return entries;
    }
  }
}
