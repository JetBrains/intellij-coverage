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

package com.intellij.rt.coverage.util;

import com.intellij.rt.coverage.data.*;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

public class InstructionsSection extends ClassListSection {
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
    return !projectData.isSampling() && projectData.isInstructionsCoverageEnabled();
  }

  @Override
  protected void loadClass(DataInputStream in, ClassData classData, int version) throws IOException {
    final LineData[] lines = (LineData[]) classData.getLines();
    for (LineData lineData : lines) {
      if (lineData == null) continue;
      lineData.addInstructions(CoverageIOUtil.readINT(in));
      for (int i = 0; i < lineData.jumpsCount(); i++) {
        final JumpData jumpData = lineData.getJumpData(i);
        jumpData.addInstructions(true, CoverageIOUtil.readINT(in));
        jumpData.addInstructions(false, CoverageIOUtil.readINT(in));
      }
      for (int i = 0; i < lineData.switchesCount(); i++) {
        final SwitchData switchData = lineData.getSwitchData(i);
        for (int key = -1; key < switchData.getKeys().length; key++) {
          switchData.addInstructions(key, CoverageIOUtil.readINT(in));
        }
      }
    }
  }

  @Override
  protected void saveClass(ClassData classData, DataOutput out, int index) throws IOException {
    final LineData[] lines = (LineData[]) classData.getLines();
    if (lines == null) return;
    CoverageIOUtil.writeINT(out, index);

    for (LineData lineData : lines) {
      if (lineData == null) continue;
      CoverageIOUtil.writeINT(out, lineData.getInstructions());
      for (int i = 0; i < lineData.jumpsCount(); i++) {
        final JumpData jumpData = lineData.getJumpData(i);
        CoverageIOUtil.writeINT(out, jumpData.getInstructions(true));
        CoverageIOUtil.writeINT(out, jumpData.getInstructions(false));
      }
      for (int i = 0; i < lineData.switchesCount(); i++) {
        final SwitchData switchData = lineData.getSwitchData(i);
        for (int key = -1; key < switchData.getKeys().length; key++) {
          CoverageIOUtil.writeINT(out, switchData.getInstructions(key));
        }
      }
    }
  }
}
