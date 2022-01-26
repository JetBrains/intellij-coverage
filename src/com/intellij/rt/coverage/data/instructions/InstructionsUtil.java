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

import java.util.Map;

public class InstructionsUtil {
  public static void merge(ProjectData source, ProjectData target) {
    if (!target.isInstructionsCoverageEnabled()) return;
    final Map<String, ClassInstructions> instructions = target.getInstructions();
    for (Map.Entry<String, ClassInstructions> entry : source.getInstructions().entrySet()) {
      final String key = entry.getKey();
      final ClassInstructions mergedInstructions = entry.getValue();
      ClassInstructions classInstructions = instructions.get(key);
      if (classInstructions == null) {
        classInstructions = new ClassInstructions();
        instructions.put(key, classInstructions);
      }
      classInstructions.merge(mergedInstructions);
    }
  }


  public static void applyInstructionsSMAP(ProjectData projectData, LineMapData[] linesMap, String sourceClass, String targetClass) {
    if (!projectData.isInstructionsCoverageEnabled()) return;
    final int maxMappedSourceLineNumber = ClassData.maxSourceLineNumber(linesMap);
    final ClassInstructions old = projectData.getInstructions().get(targetClass);
    final LineInstructions[] lineInstructions;
    if (sourceClass == targetClass || old == null || old.getlines().length == 0) {
      lineInstructions = new LineInstructions[1 + maxMappedSourceLineNumber];
    } else {
      int size = Math.max(1 + maxMappedSourceLineNumber, old.getlines().length);
      lineInstructions = new LineInstructions[size];
      System.arraycopy(old.getlines(), 0, lineInstructions, 0, old.getlines().length);
    }
    final LineInstructions[] otherLines = projectData.getInstructions().get(sourceClass).getlines();
    for (final LineMapData mapData : linesMap) {
      if (mapData == null) continue;
      int sourceLineNumber = mapData.getSourceLineNumber();
      if (lineInstructions[sourceLineNumber] == null
          && mapData.getTargetMinLine() < otherLines.length
          && otherLines[mapData.getTargetMinLine()] != null) {
        lineInstructions[sourceLineNumber] = new LineInstructions();
      }
      for (int i = mapData.getTargetMinLine(); i <= mapData.getTargetMaxLine(); i++) {
        if (i < 0 || i >= otherLines.length) continue;
        if (lineInstructions[sourceLineNumber] != null && otherLines[i] != null) {
          lineInstructions[sourceLineNumber].merge(otherLines[i]);
        }
        otherLines[i] = null;
      }
    }
    projectData.getInstructions().put(targetClass, new ClassInstructions(lineInstructions));
  }

  public static void applyInstructionsSMAPUnloaded(ProjectData projectData, String className, FileMapData[] mappings) {
    if (!projectData.isInstructionsCoverageEnabled()) return;
    final ClassInstructions classInstructions = projectData.getInstructions().get(className);
    final LineInstructions[] instructions = classInstructions.getlines();
    for (FileMapData mapData : mappings) {
      final boolean isThisClass = className.equals(mapData.getClassName());
      for (LineMapData lineMapData : mapData.getLines()) {
        final int sourceLineNumber = lineMapData.getSourceLineNumber();
        for (int i = lineMapData.getTargetMinLine(); i <= lineMapData.getTargetMaxLine() && i < instructions.length; i++) {
          final LineInstructions lineInstructions = instructions[i];
          instructions[i] = null;
          if (isThisClass && lineInstructions != null && sourceLineNumber < instructions.length) {
            if (instructions[sourceLineNumber] == null) {
              instructions[sourceLineNumber] = lineInstructions;
            } else {
              instructions[sourceLineNumber].merge(lineInstructions);
            }
          }
        }
      }
    }
  }
}
