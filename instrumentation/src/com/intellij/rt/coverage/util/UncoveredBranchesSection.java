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
import com.intellij.rt.coverage.instrumentation.InstrumentationOptions;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

/**
 * This section saves info about jumps and switches that are not saved in the main report part as line's hits counter is 0.
 * The section contains all the classes that have uncovered lines and lists jumps and switches count for such lines.
 */
public class UncoveredBranchesSection extends ClassListSection {
  @Override
  public int getId() {
    return ReportSectionsUtil.UNCOVERED_BRANCHES_SECTION_ID;
  }

  @Override
  public int getVersion() {
    return 0;
  }

  @Override
  public boolean isEngaged(ProjectData projectData, InstrumentationOptions options) {
    return options.isBranchCoverage;
  }

  @Override
  protected void loadClass(DataInputStream in, ClassData classData, int version) throws IOException {
    for (Object object : classData.getLines()) {
      final LineData line = (LineData) object;
      if (line == null || line.getHits() > 0) continue;

      final int jumpsNumber = CoverageIOUtil.readINT(in);
      for (int jumpId = 0; jumpId < jumpsNumber; jumpId++) {
        line.addJump(jumpId);
      }

      final int switchesNumber = CoverageIOUtil.readINT(in);
      for (int switchId = 0; switchId < switchesNumber; switchId++) {
        final int keysLength = CoverageIOUtil.readINT(in);
        try {
          final int[] keys = new int[keysLength];
          for (int k = 0; k < keysLength; k++) {
            keys[k] = k;
          }
          line.addSwitch(switchId, keys);
        } catch (OutOfMemoryError e) {
          ErrorReporter.error("OOM during " + classData + " class loading from report, cannot create switch with " + keysLength + " keys");
          throw e;
        }
      }

      line.fillArrays();
    }
  }

  @Override
  protected void saveClass(ClassData classData, DataOutput out, int index) throws IOException {
    int line = 0;
    final LineData[] lines = (LineData[]) classData.getLines();
    if (lines == null) return;
    for (; line < lines.length; line++) {
      final LineData lineData = lines[line];
      if (lineData == null) continue;
      if (lineData.getHits() == 0) break;
    }
    if (line == lines.length) return;
    CoverageIOUtil.writeINT(out, index);
    for (; line < lines.length; line++) {
      final LineData lineData = lines[line];
      if (lineData == null || lineData.getHits() > 0) continue;
      CoverageIOUtil.writeINT(out, lineData.jumpsCount());
      CoverageIOUtil.writeINT(out, lineData.switchesCount());
      for (int i = 0; i < lineData.switchesCount(); i++) {
        CoverageIOUtil.writeINT(out, lineData.getSwitchData(i).getHits().length);
        // keys are not saved
      }
    }
  }
}
