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

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.gnu.trove.TObjectIntHashMap;
import org.jetbrains.coverage.gnu.trove.TObjectIntProcedure;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

public class UncoveredBranchesSection extends ReportSection {
  @Override
  public int getId() {
    return ReportSectionsUtil.UNCOVERED_BRANCHES_SECTION_ID;
  }

  @Override
  public int getVersion() {
    return 0;
  }

  @Override
  public boolean isEngaged(ProjectData projectData) {
    return true;
  }

  @Override
  public void load(ProjectData projectData, DataInputStream in, TIntObjectHashMap<ClassData> dict, int version) throws IOException {
    int classId = CoverageIOUtil.readINT(in);
    while (classId != -1) {
      final ClassData classData = dict.get(classId);
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
          final int[] keys = new int[keysLength];
          for (int k = 0; k < keysLength; k++) {
            keys[k] = k;
          }
          line.addSwitch(switchId, keys);
        }

        line.fillArrays();
      }
      classId = CoverageIOUtil.readINT(in);
    }
  }

  @Override
  protected void saveInternal(final ProjectData projectData, final DataOutput out, TObjectIntHashMap<String> dict) throws IOException {
    try {
      dict.forEachEntry(new TObjectIntProcedure<String>() {
        public boolean execute(String className, int index) {
          try {
            saveClass(projectData.getClassData(className), out, index);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return true;
        }
      });
    } catch (RuntimeException e) {
      if (e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      } else {
        throw e;
      }
    }
    CoverageIOUtil.writeINT(out, -1);
  }

  private void saveClass(ClassData classData, DataOutput out, int index) throws IOException {
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
      }
    }
  }
}
