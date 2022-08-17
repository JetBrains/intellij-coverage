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
import com.intellij.rt.coverage.data.JumpData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.SwitchData;
import org.jetbrains.coverage.gnu.trove.TIntArrayList;

public class ClassInstructions {
  private static final LineInstructions[] EMPTY_LINES = new LineInstructions[0];
  private LineInstructions[] myLines;

  public ClassInstructions() {
    myLines = EMPTY_LINES;
  }

  public ClassInstructions(LineInstructions[] lines) {
    myLines = lines;
  }

  public ClassInstructions(ClassData data, TIntArrayList instructions) {
    if (data.getLines() == null) {
      myLines = EMPTY_LINES;
      return;
    }
    final int size = data.getLines().length;
    myLines = new LineInstructions[size];
    for (int line = 0; line < size; line++) {
      final LineData lineData = data.getLineData(line);
      if (lineData == null) continue;
      final LineInstructions lineInstructions = new LineInstructions();
      myLines[line] = lineInstructions;
      lineInstructions.setInstructions(instructions.get(lineData.getId()));

      final JumpData[] jumps = lineData.getJumps();
      if (jumps != null) {
        for (JumpData jumpData : jumps) {
          final JumpInstructions jumpInstructions = new JumpInstructions();
          jumpInstructions.setInstructions(true, instructions.get(jumpData.getId(true)));
          jumpInstructions.setInstructions(false, instructions.get(jumpData.getId(false)));
          lineInstructions.addJump(jumpInstructions);
        }
      }

      final SwitchData[] switches = lineData.getSwitches();
      if (switches != null) {
        for (SwitchData switchData : switches) {
          final SwitchInstructions switchInstructions = new SwitchInstructions(switchData.getKeys().length);
          for (int i = -1; i < switchData.getKeys().length; i++) {
            switchInstructions.setInstructions(i, instructions.get(switchData.getId(i)));
          }
          lineInstructions.addSwitch(switchInstructions);
        }
      }
    }
  }

  public LineInstructions[] getlines() {
    return myLines;
  }

  public void merge(ClassInstructions other, ClassData classData) {
    if (myLines.length < other.myLines.length) {
      final LineInstructions[] old = myLines;
      myLines = new LineInstructions[other.myLines.length];
      System.arraycopy(old, 0, myLines, 0, old.length);
    }

    for (int i = 0; i < other.myLines.length; i++) {
      if (other.myLines[i] == null) continue;
      if (myLines[i] == null) {
        if (classData.isIgnoredLine(i)) continue;
        myLines[i] = new LineInstructions();
      }
      myLines[i].merge(other.myLines[i]);
    }
  }
}
