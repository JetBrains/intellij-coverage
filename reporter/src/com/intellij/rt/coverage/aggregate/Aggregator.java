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

import com.intellij.rt.coverage.aggregate.api.Request;
import com.intellij.rt.coverage.data.*;
import com.intellij.rt.coverage.instrumentation.InstrumentationOptions;
import com.intellij.rt.coverage.instrumentation.UnloadedUtil;
import com.intellij.rt.coverage.instrument.RawReportLoader;
import com.intellij.rt.coverage.instrumentation.data.ProjectContext;
import com.intellij.rt.coverage.report.data.BinaryReport;
import com.intellij.rt.coverage.util.CoverageReport;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import com.intellij.rt.coverage.util.classFinder.ClassFilter;
import com.intellij.rt.coverage.util.classFinder.OutputClassFinder;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Collects results from different modules into a number of intermediate binary reports and collects coverage in unloaded classes
 */
public class Aggregator {
  private final List<BinaryReport> myReports;
  private final List<File> myOutputs;
  private final List<Request> myRequests;

  public Aggregator(List<BinaryReport> reports, List<File> outputRoots, List<Request> requests) {
    myReports = reports;
    myOutputs = outputRoots;
    myRequests = requests;
  }

  public Aggregator(List<BinaryReport> reports, List<File> outputRoots, Request request) {
    this(reports, outputRoots, Collections.singletonList(request));
  }

  /**
   * Collect a merged project data from all output roots and all binary reports.
   * Collecting this data once ensures that unloaded classes will not be analysed several times.
   */
  public ProjectData getProjectData(Request request) {
    boolean hasRawHitsReport = false;
    for (BinaryReport report : myReports) {
      hasRawHitsReport |= report.isRawHitsReport();
    }
    // Note that instructions collection is done only inside this method
    // to ensure that instructions count in inline methods
    // correspond to method definition, not method call
    ProjectData projectData = new ProjectData();
    final ProjectContext context = collectCoverageInformationFromOutputs(projectData, request);
    final ProjectData projectDataCopy = hasRawHitsReport ? copyProjectData(projectData) : null;
    context.dropLineMappings(projectData);

    for (BinaryReport report : myReports) {
      if (report.isRawHitsReport()) {
        try {
          RawReportLoader.load(report.getDataFile(), projectDataCopy);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else {
        final ProjectData data = ProjectDataLoader.load(report.getDataFile());
        context.dropIgnoredLines(data);
        mergeHits(projectData, data);
      }
    }
    if (projectDataCopy != null) {
      context.finalizeCoverage(projectDataCopy);
      mergeHits(projectData, projectDataCopy);
    }

    if (context.getInherits() != null) {
      projectData = filterInheritance(request, context, projectData);
    }

    return projectData;
  }

  private static ProjectData filterInheritance(Request request, ProjectContext context, ProjectData projectData) {
    InheritanceFilter filter = new InheritanceFilter(context.getInherits());
    List<String> filteredNames = filter.filterInherits(projectData.getClasses().keySet(), request.filters.includeInherits, request.filters.excludeInherits);
    ProjectData copy = new ProjectData();
    for (String className : filteredNames) {
      if (!request.classFilter.shouldInclude(className)) continue;
      ClassData classData = projectData.getClassData(className);
      if (classData != null) {
        copy.addClassData(classData);
      }
    }
    return copy;
  }

  private static ProjectData copyProjectData(ProjectData projectData) {
    final ProjectData projectDataCopy = new ProjectData();
    for (ClassData classData : projectData.getClassesCollection()) {
      final ClassData classCopy = projectDataCopy.getOrCreateClassData(classData.getName());
      final LineData[] lines = (LineData[]) classData.getLines();
      if (lines == null) continue;
      final LineData[] linesCopy = new LineData[lines.length];
      classCopy.setLines(linesCopy);
      for (LineData lineData : lines) {
        if (lineData == null) continue;
        final LineData lineCopy = new LineData(lineData.getLineNumber(), lineData.getMethodSignature());
        lineCopy.setId(lineData.getId());
        linesCopy[lineCopy.getLineNumber()] = lineCopy;

        final JumpData[] jumps = lineData.getJumps();
        if (jumps != null) {
          for (int i = 0; i < jumps.length; i++) {
            final JumpData jump = jumps[i];
            final JumpData jumpCopy = lineCopy.addJump(i);
            jumpCopy.setId(jump.getId(true), true);
            jumpCopy.setId(jump.getId(false), false);
          }
        }

        final SwitchData[] switches = lineData.getSwitches();
        if (switches != null) {
          for (int i = 0; i < switches.length; i++) {
            final SwitchData aSwitch = switches[i];
            final SwitchData switchCopy = lineCopy.addSwitch(i, aSwitch.getKeys());
            for (int key = -1; key < aSwitch.getKeys().length; key++) {
              switchCopy.setId(aSwitch.getId(key), key);
            }
          }
        }
        lineCopy.fillArrays();
      }
    }
    return projectDataCopy;
  }

  private static void mergeHits(ProjectData dst, ProjectData src) {
    for (ClassData srcClass : src.getClassesCollection()) {
      final ClassData dstClass = dst.getClassData(srcClass.getName());
      if (dstClass == null) {
        // dst ProjectData contains all classes already filtered by outputs and filters
        // so this class must be filtered
        continue;
      }
      dstClass.merge(srcClass);
    }
  }

  /**
   * Processing request is selecting required classes from a global project data.
   */
  public void processRequests() {
    for (Request request : myRequests) {
      if (request.outputFile == null) continue;
      ProjectData projectData = getProjectData(request);
      InstrumentationOptions options = new InstrumentationOptions.Builder()
          .setDataFile(request.outputFile)
          .setSourceMapFile(request.smapFile)
          .build();
      CoverageReport.save(projectData, options);
    }
  }

  private ProjectContext collectCoverageInformationFromOutputs(ProjectData projectData, Request request) {
    projectData.setInstructionsCoverage(true);
    InstrumentationOptions options = new InstrumentationOptions.Builder()
        .setBranchCoverage(true)
        .setSaveSource(true)
        .setInstructionCoverage(true)
        .setIncludeAnnotations(request.filters.includeAnnotations)
        .setExcludeAnnotations(request.filters.excludeAnnotations)
        .build();
    boolean collectInherits = request.filters.shouldCheckInherits();
    // Cannot filter classes by name, as we need to collect inheritance hierarchy.
    // Class pattern will be applied later.
    ClassFilter.PatternFilter classFilter = collectInherits ? null : request.classFilter;
    ProjectContext context = new ProjectContext(options, new OutputClassFinder(classFilter, myOutputs));
    context.setCollectInherits(collectInherits);
    UnloadedUtil.appendUnloaded(projectData, context);
    return context;
  }

}
