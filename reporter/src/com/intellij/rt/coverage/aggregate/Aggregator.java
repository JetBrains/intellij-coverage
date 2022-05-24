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

package com.intellij.rt.coverage.aggregate;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.data.instructions.InstructionsUtil;
import com.intellij.rt.coverage.instrumentation.SaveHook;
import com.intellij.rt.coverage.report.data.BinaryReport;
import com.intellij.rt.coverage.report.data.Module;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import com.intellij.rt.coverage.util.classFinder.ClassFilter;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;
import com.intellij.rt.coverage.util.classFinder.ClassPathEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Aggregator {
  private final List<BinaryReport> myReports;
  private final List<Module> myModules;
  private final List<Request> myRequests;
  private ProjectData myProjectData;

  public Aggregator(List<BinaryReport> reports, List<Module> modules, List<Request> requests) {
    myReports = reports;
    myModules = modules;
    myRequests = requests;
  }

  public Aggregator(List<BinaryReport> reports, List<Module> modules, ClassFilter.PatternFilter filter) {
    this(reports, modules, Collections.singletonList(new Request(filter, null)));
  }

  public ProjectData getProjectData() {
    if (myProjectData != null) return myProjectData;
    final ProjectData projectData = collectCoverageInformationFromOutputs();
    for (BinaryReport report : myReports) {
      final ProjectData data = ProjectDataLoader.load(report.getDataFile());

      for (ClassData classData : data.getClassesCollection()) {
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

  public void processRequests() {
    final ProjectData projectData = getProjectData();

    for (Request request : myRequests) {
      if (request.outputFile == null) continue;
      final ProjectData requestProjectData = new ProjectData();
      requestProjectData.setInstructionsCoverage(true);

      for (ClassData classData : projectData.getClassesCollection()) {
        if (request.classFilter.shouldInclude(classData.getName())) {
          final ClassData newClassData = requestProjectData.getOrCreateClassData(classData.getName());
          newClassData.merge(classData);
        }
      }
      InstructionsUtil.merge(projectData, requestProjectData, request.classFilter);
      SaveHook.save(requestProjectData, request.outputFile, null);
    }
  }

  public List<File> getSources() {
    final List<File> sources = new ArrayList<File>();
    for (Module module : myModules) {
      sources.addAll(module.getSources());
    }
    return sources;
  }

  private ProjectData collectCoverageInformationFromOutputs() {
    final ProjectData projectData = new ProjectData();
    projectData.setInstructionsCoverage(true);
    SaveHook.appendUnloadedFullAnalysis(projectData, createClassFinder(), true, false, true, false);
    projectData.checkLineMappings(false);
    return projectData;
  }

  private OutputClassFinder createClassFinder() {
    final List<ClassFilter.PatternFilter> filters = new ArrayList<ClassFilter.PatternFilter>();
    for (Request request : myRequests) {
      filters.add(request.classFilter);
    }
    return new OutputClassFinder(new GroupPatternFilter(filters));
  }

  private class OutputClassFinder extends ClassFinder {

    public OutputClassFinder(ClassFilter filter) {
      super(filter);
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

  public static class Request {
    public final ClassFilter.PatternFilter classFilter;
    public final File outputFile;

    public Request(ClassFilter.PatternFilter classFilter, File outputFile) {
      this.classFilter = classFilter;
      this.outputFile = outputFile;
    }
  }
}
