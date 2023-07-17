/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

package com.intellij.rt.coverage.offline.api;

import com.intellij.rt.coverage.data.*;
import com.intellij.rt.coverage.instrument.RawReportLoader;
import com.intellij.rt.coverage.instrumentation.UnloadedUtil;
import com.intellij.rt.coverage.offline.RawProjectData;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CoverageCollector {
  static List<ClassCoverage> collect(RawProjectData rawData, ClassFinder classFinder) {
    List<ClassCoverage> classes = new ArrayList<ClassCoverage>();
    if (rawData == null) return classes;
    ProjectData projectData = collectRawData(rawData, classFinder);

    for (ClassData classData : projectData.getClassesCollection()) {
      ClassCoverage classCoverage = new ClassCoverage(classData.getName(), classData.getSource());
      classes.add(classCoverage);
      Map<String, List<LineData>> methods = classData.mapLinesToMethods();

      for (Map.Entry<String, List<LineData>> method : methods.entrySet()) {
        List<LineData> lines = method.getValue();
        MethodCoverage methodCoverage = new MethodCoverage(method.getKey(), getMethodHits(lines));
        classCoverage.methods.add(methodCoverage);
        for (LineData lineData : lines) {
          LineCoverage lineCoverage = collectLineCoverage(lineData);
          methodCoverage.lines.add(lineCoverage);
        }
      }
    }

    return classes;
  }

  private static ProjectData collectRawData(RawProjectData rawData, ClassFinder classFinder) {
    ProjectData projectData = new ProjectData();
    UnloadedUtil.appendUnloaded(projectData, classFinder, true, true);
    RawReportLoader.apply(projectData, rawData);
    projectData.applyLineMappings();
    return projectData;
  }

  private static LineCoverage collectLineCoverage(LineData lineData) {
    LineCoverage lineCoverage = new LineCoverage(lineData.getLineNumber(), lineData.getHits());
    JumpData[] jumps = lineData.getJumps();
    if (jumps != null) {
      for (JumpData jump : jumps) {
        lineCoverage.branchHits.add(jump.getTrueHits());
        lineCoverage.branchHits.add(jump.getFalseHits());
      }
    }

    SwitchData[] switches = lineData.getSwitches();
    if (switches != null) {
      for (SwitchData switchData : switches) {
        for (int hits : switchData.getHits()) {
          lineCoverage.branchHits.add(hits);
        }
        lineCoverage.branchHits.add(switchData.getDefaultHits());
      }
    }
    return lineCoverage;
  }

  private static int getMethodHits(List<LineData> lines) {
    for (LineData lineData : lines) {
      int hits = lineData.getHits();
      if (hits > 0) return hits;
    }
    return 0;
  }
}
