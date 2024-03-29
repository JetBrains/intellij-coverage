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

package com.intellij.rt.coverage.util;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.InstrumentationOptions;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.gnu.trove.TObjectIntHashMap;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportSectionsUtil {
  public static final int UNCOVERED_BRANCHES_SECTION_ID = 1;
  public static final int INSTRUCTIONS_SECTION_ID = 2;
  public static final int PARTLY_ANALYSED_CLASSES_SECTION_ID = 3;
  public static final int INCLUDE_PATTERNS_SECTION_ID = 4;

  private static Map<Integer, ReportSection> getSections(ProjectData projectData) {
    final Map<Integer, ReportSection> result = new LinkedHashMap<Integer, ReportSection>();
    result.put(UNCOVERED_BRANCHES_SECTION_ID, new UncoveredBranchesSection());
    result.put(INSTRUCTIONS_SECTION_ID, new InstructionsSection(projectData));
    result.put(PARTLY_ANALYSED_CLASSES_SECTION_ID, new PartlyAnalysedClassesSection());
    result.put(INCLUDE_PATTERNS_SECTION_ID, new IncludePatternsSection());
    return result;
  }

  public static void loadSections(ProjectData projectData, DataInputStream in, TIntObjectHashMap<ClassData> dict) throws IOException {
    final int numberOfSections = CoverageIOUtil.readINT(in);

    final Map<Integer, ReportSection> sections = getSections(projectData);
    for (int i = 0; i < numberOfSections; i++) {
      final int sectionId = CoverageIOUtil.readINT(in);
      final int size = CoverageIOUtil.readINT(in);
      final int version = CoverageIOUtil.readINT(in);
      final ReportSection section = sections.get(sectionId);

      if (section != null) {
        if (version <= section.getVersion()) {
          section.load(projectData, in, dict, version);
          continue;
        } else {
          ErrorReporter.info("Section version " + version + " is greater than agent maximum support version "
              + section.getVersion() + "\n" + "Please try to update coverage agent.");
        }
      } else {
        ErrorReporter.info("Unknown section id " + sectionId + ". Please try to update coverage agent.");
      }
      in.skipBytes(size);
    }
  }

  public static void saveSections(ProjectData projectData, DataOutputStream out,
                                  TObjectIntHashMap<String> dict, InstrumentationOptions options) throws IOException {
    List<ReportSection> sections = getEngagedSections(projectData, options);
    CoverageIOUtil.writeINT(out, sections.size());
    for (ReportSection section : sections) {
      section.save(projectData, out, dict);
    }
  }

  private static List<ReportSection> getEngagedSections(ProjectData projectData, InstrumentationOptions options) {
    List<ReportSection> engagedSections = new ArrayList<ReportSection>();
    for (ReportSection section : getSections(projectData).values()) {
      if (section.isEngaged(projectData, options)) {
        engagedSections.add(section);
      }
    }
    return engagedSections;
  }
}
