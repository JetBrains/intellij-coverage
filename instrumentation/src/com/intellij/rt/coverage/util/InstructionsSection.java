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
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.data.SwitchData;
import com.intellij.rt.coverage.data.instructions.ClassInstructions;
import com.intellij.rt.coverage.data.instructions.JumpInstructions;
import com.intellij.rt.coverage.data.instructions.LineInstructions;
import com.intellij.rt.coverage.data.instructions.SwitchInstructions;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

/**
 * Save instruction counters.
 */
public class InstructionsSection extends ClassListSection {
  final ProjectData myProjectData;

  public InstructionsSection(ProjectData projectData) {
    myProjectData = projectData;
  }

  @Override
  public int getId() {
    return ReportSectionsUtil.INSTRUCTIONS_SECTION_ID;
  }

  @Override
  public int getVersion() {
    return 0;
  }

  @Override
  public boolean isEngaged(ProjectData projectData) {
    return projectData.isBranchCoverage() && projectData.isInstructionsCoverageEnabled();
  }

  @Override
  public void load(ProjectData projectData, DataInputStream in, TIntObjectHashMap<ClassData> dict, int version) throws IOException {
    projectData.setInstructionsCoverage(true);
    super.load(projectData, in, dict, version);
  }

  @Override
  protected void loadClass(DataInputStream in, ClassData classData, int version) throws IOException {
    final LineData[] lines = (LineData[]) classData.getLines();
    final LineInstructions[] instructions = new LineInstructions[lines.length];
    for (LineData lineData : lines) {
      if (lineData == null) continue;
      final LineInstructions lineInstructions = new LineInstructions();
      instructions[lineData.getLineNumber()] = lineInstructions;
      lineInstructions.setInstructions(CoverageIOUtil.readINT(in));
      for (int i = 0; i < lineData.jumpsCount(); i++) {
        final JumpInstructions jump = new JumpInstructions();
        lineInstructions.addJump(jump);
        jump.setInstructions(true, CoverageIOUtil.readINT(in));
        jump.setInstructions(false, CoverageIOUtil.readINT(in));
      }
      for (int i = 0; i < lineData.switchesCount(); i++) {
        final SwitchData switchData = lineData.getSwitchData(i);
        final int size = switchData.getKeys().length;
        final SwitchInstructions switchInstructions = new SwitchInstructions(size);
        lineInstructions.addSwitch(switchInstructions);
        for (int key = -1; key < size; key++) {
          switchInstructions.setInstructions(key, CoverageIOUtil.readINT(in));
        }
      }
    }
    myProjectData.getInstructions().put(classData.getName(), new ClassInstructions(instructions));
  }

  @Override
  protected void saveClass(ClassData classData, DataOutput out, int index) throws IOException {
    final LineData[] lines = (LineData[]) classData.getLines();
    if (lines == null) return;
    final ClassInstructions classInstructions = myProjectData.getInstructions().get(classData.getName());
    if (classInstructions == null) {
      int lineCount = 0;
      for (LineData lineData : lines) {
        if (lineData != null) {
          lineCount++;
        }
      }
      ErrorReporter.reportError("Class " + classData.getName() + " does not have instructions info, while there are " +
          lineCount + " lines available", new Throwable());
      return;
    }
    CoverageIOUtil.writeINT(out, index);
    final LineInstructions[] lineInstructions = classInstructions.getlines();
    for (int line = 0; line < lines.length; line++) {
      final LineData lineData = lines[line];
      if (lineData == null) continue;
      final LineInstructions lineInstruction = line >= lineInstructions.length ? null : lineInstructions[line];
      CoverageIOUtil.writeINT(out, lineInstruction == null ? 0 : lineInstruction.getInstructions());
      final List<JumpInstructions> jumps = lineInstruction == null ? null : lineInstruction.getJumps();
      for (int i = 0; i < lineData.jumpsCount(); i++) {
        final JumpInstructions jumpInstructions = jumps == null || i >= jumps.size() ? null : jumps.get(i);
        CoverageIOUtil.writeINT(out, jumpInstructions == null ? 0 : jumpInstructions.getInstructions(true));
        CoverageIOUtil.writeINT(out, jumpInstructions == null ? 0 : jumpInstructions.getInstructions(false));
      }
      final List<SwitchInstructions> switches = lineInstruction == null ? null : lineInstruction.getSwitches();
      for (int i = 0; i < lineData.switchesCount(); i++) {
        final SwitchInstructions switchInstructions = switches == null || i >= switches.size() ? null : switches.get(i);
        for (int key = -1; key < lineData.getSwitchData(i).getKeys().length; key++) {
          CoverageIOUtil.writeINT(out, switchInstructions == null ? 0 : switchInstructions.getInstructions(key));
        }
      }
    }
  }
}
