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

package com.intellij.rt.coverage.data.instructions;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.FileMapData;
import com.intellij.rt.coverage.data.LineMapData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.util.LineMapper;
import com.intellij.rt.coverage.util.classFinder.ClassFilter;

import java.util.Map;

public class InstructionsUtil {
  public static void merge(ProjectData source, ProjectData target, ClassFilter classFilter) {
    if (!target.isInstructionsCoverageEnabled()) return;
    final Map<String, ClassInstructions> instructions = target.getInstructions();
    for (Map.Entry<String, ClassInstructions> entry : source.getInstructions().entrySet()) {
      final String className = entry.getKey();
      if (classFilter != null && !classFilter.shouldInclude(className)) continue;
      final ClassData classData = target.getClassData(className);
      if (classData == null) continue;
      final ClassInstructions mergedInstructions = entry.getValue();
      ClassInstructions classInstructions = instructions.get(className);
      if (classInstructions == null) {
        classInstructions = new ClassInstructions();
        instructions.put(className, classInstructions);
      }
      classInstructions.merge(mergedInstructions, classData);
    }
  }

  public static void applyInstructionsSMAP(ProjectData projectData, LineMapData[] linesMap, ClassData sourceClass, ClassData targetClass) {
    if (!projectData.isInstructionsCoverageEnabled()) return;
    final LineMapper<LineInstructions> mapper = new InstructionsLineMapper(projectData);
    final LineInstructions[] lines = mapper.mapLines(linesMap, sourceClass, targetClass);
    projectData.getInstructions().put(sourceClass.getName(), new ClassInstructions(lines));
  }

  public static void dropMappedLines(ProjectData projectData, String className, FileMapData[] mappings) {
    if (!projectData.isInstructionsCoverageEnabled()) return;
    final ClassInstructions classInstructions = projectData.getInstructions().get(className);
    if (classInstructions == null) return;
    final LineInstructions[] instructions = classInstructions.getlines();
    LineMapper.dropMappedLines(mappings, instructions, className);
  }

  private static class InstructionsLineMapper extends LineMapper<LineInstructions> {

    private final ProjectData projectData;

    public InstructionsLineMapper(ProjectData projectData) {
      this.projectData = projectData;
    }

    @Override
    protected LineInstructions createNewLine(LineInstructions targetLine, int line) {
      return new LineInstructions();
    }

    @Override
    protected LineInstructions[] createArray(int size) {
      return new LineInstructions[size];
    }

    @Override
    protected LineInstructions[] getLines(ClassData classData) {
      final ClassInstructions classInstructions = projectData.getInstructions().get(classData.getName());
      if (classInstructions == null) return null;
      return classInstructions.getlines();
    }
  }
}
