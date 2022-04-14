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

package com.intellij.rt.coverage.verify;

import com.intellij.rt.coverage.data.BranchData;
import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.data.instructions.ClassInstructions;
import com.intellij.rt.coverage.data.instructions.LineInstructions;

public class ProjectTargetProcessor implements TargetProcessor {

  @Override
  public void process(ProjectData projectData, Consumer consumer) {
    final Verifier.CollectedCoverage coverage = new Verifier.CollectedCoverage();
    for (ClassData classData : projectData.getClassesCollection()) {
      coverage.add(collectClassCoverage(projectData, classData));
    }
    consumer.consume("all", coverage);
  }


  public static Verifier.CollectedCoverage collectClassCoverage(ProjectData projectData, ClassData classData) {
    final ClassInstructions classInstructions = projectData.isInstructionsCoverageEnabled()
        ? projectData.getInstructions().get(classData.getName())
        : null;
    final LineInstructions[] instructions = classInstructions == null ? null : classInstructions.getlines();

    final Verifier.CollectedCoverage coverage = new Verifier.CollectedCoverage();
    final Object[] lines = classData.getLines();
    if (lines == null) return coverage;
    for (LineData lineData : (LineData[]) lines) {
      if (lineData == null) continue;
      if (lineData.getHits() > 0) {
        coverage.lineCounter.covered++;
      } else {
        coverage.lineCounter.missed++;
      }

      final BranchData branchData = lineData.getBranchData();
      if (branchData != null) {
        coverage.branchCounter.covered += branchData.getCoveredBranches();
        coverage.branchCounter.missed += branchData.getTotalBranches() - branchData.getCoveredBranches();
      }

      final LineInstructions lineInstructions = instructions == null ? null : instructions[lineData.getLineNumber()];
      if (lineInstructions != null) {
        final BranchData instructionsData = lineInstructions.getInstructionsData(lineData);
        coverage.instructionCounter.covered = instructionsData.getCoveredBranches();
        coverage.instructionCounter.missed = instructionsData.getTotalBranches() - instructionsData.getCoveredBranches();
      }
    }

    return coverage;
  }
}
