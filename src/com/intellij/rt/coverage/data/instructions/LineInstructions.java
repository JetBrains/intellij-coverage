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

import com.intellij.rt.coverage.data.BranchData;
import com.intellij.rt.coverage.data.JumpData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.SwitchData;

import java.util.ArrayList;
import java.util.List;

public class LineInstructions {
  private int myInstructions;
  private List<JumpInstructions> myJumps;
  private List<SwitchInstructions> mySwitches;

  public void setInstructions(int instructions) {
    myInstructions = instructions;
  }

  public void addJump(JumpInstructions jump) {
    if (myJumps == null) {
      myJumps = new ArrayList<JumpInstructions>();
    }
    myJumps.add(jump);
  }

  public void addSwitch(SwitchInstructions aSwitch) {
    if (mySwitches == null) {
      mySwitches = new ArrayList<SwitchInstructions>();
    }
    mySwitches.add(aSwitch);
  }

  public int getInstructions() {
    return myInstructions;
  }

  public List<JumpInstructions> getJumps() {
    return myJumps;
  }

  public List<SwitchInstructions> getSwitches() {
    return mySwitches;
  }

  public void merge(LineInstructions other) {
    myInstructions = Math.max(myInstructions, other.myInstructions);

    if (other.myJumps != null) {
      if (myJumps == null) {
        myJumps = new ArrayList<JumpInstructions>();
      }
      for (int i = myJumps.size(); i < other.myJumps.size(); i++) {
        myJumps.add(new JumpInstructions());
      }
      for (int i = 0; i < other.myJumps.size(); i++) {
        myJumps.get(i).merge(other.myJumps.get(i));
      }
    }


    if (other.mySwitches != null) {
      if (mySwitches == null) {
        mySwitches = new ArrayList<SwitchInstructions>();
      }
      for (int i = mySwitches.size(); i < other.mySwitches.size(); i++) {
        mySwitches.add(new SwitchInstructions(other.mySwitches.get(i).size()));
      }
      for (int i = 0; i < other.mySwitches.size(); i++) {
        mySwitches.get(i).merge(other.mySwitches.get(i));
      }
    }
  }

  public BranchData getInstructionsData(LineData line) {
    int total = 0;
    int covered = 0;

    total += myInstructions;
    if (line.getHits() > 0) covered += myInstructions;

    final JumpData[] jumps = line.getJumps();
    if (jumps != null && myJumps != null) {
      for (int i = 0; i < Math.min(jumps.length, myJumps.size()); i++) {
        final JumpData jump = jumps[i];
        final JumpInstructions jumpInstructions = myJumps.get(i);
        total += jumpInstructions.getInstructions(true) + jumpInstructions.getInstructions(false);
        if (jump.getTrueHits() > 0) covered += jumpInstructions.getInstructions(true);
        if (jump.getFalseHits() > 0) covered += jumpInstructions.getInstructions(false);
      }
    }

    final SwitchData[] switches = line.getSwitches();
    if (switches != null && mySwitches != null) {
      for (int i = 0; i < Math.min(switches.length, mySwitches.size()); i++) {
        final SwitchData switchData = switches[i];
        final SwitchInstructions switchInstructions = mySwitches.get(i);
        total += switchInstructions.getInstructions(-1);
        if (switchData.getDefaultHits() > 0) covered += switchInstructions.getInstructions(-1);
        for (int key = 0; key < switchData.getKeys().length; key++) {
          total += switchInstructions.getInstructions(key);
          if (switchData.getHits()[key] > 0) covered += switchInstructions.getInstructions(key);
        }
      }
    }

    return new BranchData(total, covered);
  }
}
