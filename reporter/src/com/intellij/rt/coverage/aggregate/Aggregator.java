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
import com.intellij.rt.coverage.data.instructions.InstructionsUtil;
import com.intellij.rt.coverage.instrumentation.UnloadedUtil;
import com.intellij.rt.coverage.instrument.RawReportLoader;
import com.intellij.rt.coverage.report.data.BinaryReport;
import com.intellij.rt.coverage.util.CoverageReport;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import com.intellij.rt.coverage.util.classFinder.ClassFilter;
import com.intellij.rt.coverage.util.classFinder.OutputClassFinder;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Collects results from different modules into a number of intermediate binary reports and collects coverage in unloaded classes
 */
public class Aggregator {
  private final List<BinaryReport> myReports;
  private final List<File> myOutputs;
  private final List<Request> myRequests;

  /** This is a merged project data for all requests. */
  private ProjectData myProjectData;

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
  public ProjectData getProjectData() {
    if (myProjectData != null) return myProjectData;
    boolean hasRawHitsReport = false;
    for (BinaryReport report : myReports) {
      hasRawHitsReport |= report.isRawHitsReport();
    }
    // Note that instructions collection is done only inside this method
    // to ensure that instructions count in inline methods
    // correspond to method definition, not method call
    final ProjectData projectData = collectCoverageInformationFromOutputs();
    final ProjectData projectDataCopy = hasRawHitsReport ? copyProjectData(projectData) : null;
    projectData.dropLineMappings();

    for (BinaryReport report : myReports) {
      if (report.isRawHitsReport()) {
        try {
          RawReportLoader.load(report.getDataFile(), projectDataCopy);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else {
        final ProjectData data = ProjectDataLoader.load(report.getDataFile());
        mergeHits(projectData, data);
      }
    }
    if (projectDataCopy != null) {
      projectDataCopy.applyLineMappings();
      mergeHits(projectData, projectDataCopy);
    }
    myProjectData = projectData;
    return projectData;
  }

  private static ProjectData copyProjectData(ProjectData projectData) {
    final ProjectData projectDataCopy = ProjectData.createProjectData(true);
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
    final Map<String, FileMapData[]> mappings = projectData.getLinesMap();
    if (mappings != null) {
      for (Map.Entry<String, FileMapData[]> entry : mappings.entrySet()) {
        projectDataCopy.addLineMaps(entry.getKey(), entry.getValue());
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
      CoverageReport.save(requestProjectData, request.outputFile, request.smapFile);
    }
  }

  private ProjectData collectCoverageInformationFromOutputs() {
    final ProjectData projectData = new ProjectData();
    final List<Pattern> excludeAnnotations = new ArrayList<Pattern>();
    for (Request request : myRequests) {
      excludeAnnotations.addAll(request.excludeAnnotations);
    }
    projectData.setInstructionsCoverage(true);
    projectData.setAnnotationsToIgnore(excludeAnnotations);
    UnloadedUtil.appendUnloaded(projectData, createClassFinder(), true, true);
    return projectData;
  }

  private OutputClassFinder createClassFinder() {
    final List<ClassFilter.PatternFilter> filters = new ArrayList<ClassFilter.PatternFilter>();
    for (Request request : myRequests) {
      filters.add(request.classFilter);
    }
    return new OutputClassFinder(new GroupPatternFilter(filters), myOutputs);
  }
}
