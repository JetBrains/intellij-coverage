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

package com.intellij.rt.coverage.instrumentation.data;

import com.intellij.rt.coverage.data.JumpData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.SwitchData;
import com.intellij.rt.coverage.instrumentation.Instrumenter;
import org.jetbrains.coverage.org.objectweb.asm.Label;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BranchDataContainer {
  private final Instrumenter myContext;

  private int myNextId = 0;

  private Label myLastFalseJump;
  private Label myLastTrueJump;

  private Map<Label, Jump> myJumps;
  private Map<Label, Switch> mySwitches;

  private HashMap<Label, SwitchData> myDefaultTableSwitchLabels;

  public BranchDataContainer(Instrumenter context) {
    myContext = context;
  }

  public int getSize() {
    return myNextId;
  }

  public void resetMethod() {
    myLastFalseJump = null;
    myLastTrueJump = null;
    if (myJumps != null) myJumps.clear();
    if (mySwitches != null) mySwitches.clear();
    myDefaultTableSwitchLabels = null;
  }

  public Instrumenter getContext() {
    return myContext;
  }

  public Jump getJump(Label jump) {
    if (myJumps == null) return null;
    return myJumps.get(jump);
  }

  public Switch getSwitch(Label label) {
    if (mySwitches == null) return null;
    return mySwitches.get(label);
  }

  public void addLine(LineData lineData) {
    int id = lineData.getId();
    if (id == -1) {
      id = myNextId++;
      lineData.setId(id);
    }
  }

  public void addJump(LineData lineData, int index, Label trueLabel, Label falseLabel) {
    int line = lineData.getLineNumber();
    // jump type is inverted as jump occurs if value is true
    Jump trueJump = new Jump(myNextId++, index, line, false);
    Jump falseJump = new Jump(myNextId++, index, line, true);
    myLastTrueJump = trueLabel;
    myLastFalseJump = falseLabel;

    if (myJumps == null) myJumps = new HashMap<Label, Jump>();
    myJumps.put(falseLabel, falseJump);
    myJumps.put(trueLabel, trueJump);

    JumpData jumpData = lineData.addJump(index);
    jumpData.setId(trueJump.getId(), trueJump.getType());
    jumpData.setId(falseJump.getId(), falseJump.getType());
  }

  public void addLookupSwitch(LineData lineData, int index, Label dflt, int[] keys, Label[] labels) {
    List<Switch> switches = rememberSwitchLabels(lineData.getLineNumber(), dflt, labels, index);
    SwitchData switchData = lineData.addSwitch(index, keys);
    setSwitchIds(switchData, switches);
  }

  public void addTableSwitch(LineData lineData, int index, int min, int max, Label dflt, Label[] labels, Label originalDefault) {
    List<Switch> switches = rememberSwitchLabels(lineData.getLineNumber(), dflt, labels, index);
    SwitchData switchData = lineData.addSwitch(index, min, max);
    setSwitchIds(switchData, switches);
    if (myDefaultTableSwitchLabels == null) myDefaultTableSwitchLabels = new HashMap<Label, SwitchData>();
    myDefaultTableSwitchLabels.put(originalDefault, switchData);
  }

  public void removeLastJump() {
    if (myLastTrueJump == null) return;
    myJumps.remove(myLastFalseJump);
    Jump trueJump = myJumps.remove(myLastTrueJump);
    myLastTrueJump = null;
    myLastFalseJump = null;

    if (trueJump == null) return;
    LineData lineData = myContext.getLineData(trueJump.getLine());
    if (lineData == null) return;
    lineData.removeJump(lineData.jumpsCount() - 1);
  }

  public void removeLastSwitch(Label dflt, Label... labels) {
    if (mySwitches == null) return;
    Switch aSwitch = mySwitches.remove(dflt);
    for (Label label : labels) {
      mySwitches.remove(label);
    }
    if (aSwitch == null) return;
    final LineData lineData = myContext.getLineData(aSwitch.getLine());
    if (lineData == null) return;
    lineData.removeSwitch(lineData.switchesCount() - 1);
  }

  public Map<Label, SwitchData> getDefaultTableSwitchLabels() {
    return myDefaultTableSwitchLabels;
  }

  private List<Switch> rememberSwitchLabels(final int line, final Label dflt, final Label[] labels, int switchIndex) {
    List<Switch> result = new ArrayList<Switch>();
    if (mySwitches == null) mySwitches = new HashMap<Label, Switch>();

    Switch aSwitch = new Switch(myNextId++, switchIndex, line, -1);
    result.add(aSwitch);
    mySwitches.put(dflt, aSwitch);

    for (int i = labels.length - 1; i >= 0; i--) {
      aSwitch = new Switch(myNextId++, switchIndex, line, i);
      result.add(aSwitch);
      mySwitches.put(labels[i], aSwitch);
    }

    return result;
  }

  private void setSwitchIds(SwitchData data, List<Switch> switches) {
    for (Switch aSwitch : switches) {
      data.setId(aSwitch.getId(), aSwitch.getKey());
    }
  }
}
