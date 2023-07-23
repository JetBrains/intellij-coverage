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
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.data.SwitchData;
import com.intellij.rt.coverage.instrumentation.filters.KotlinUtils;
import org.jetbrains.coverage.gnu.trove.TIntArrayList;
import org.jetbrains.coverage.gnu.trove.TIntHashSet;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.org.objectweb.asm.Label;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Storage for Label to jump/switch mapping.
 * This class is used to set branch ids during instrumentation.
 */
public class InstrumentationData {
  // Class level data
  private final TIntObjectHashMap<LineData> myLines = new TIntObjectHashMap<LineData>();
  private final Map<Key<?>, Object> myProperties = new HashMap<Key<?>, Object>();
  private final TIntArrayList myInstructions;
  private TIntHashSet myIgnoredLines;
  private int myIgnoreSection = 0;
  private int myNextId = 0;
  private int myMaxSeenLine;


  // Method level data
  private boolean myLinesInCurrentMethod;
  private Label myLastFalseJump;
  private Label myLastTrueJump;
  private Map<Label, Jump> myJumps;
  private Map<Label, Switch> mySwitches;

  public InstrumentationData(ProjectData projectData) {
    myInstructions = projectData.isInstructionsCoverageEnabled() ? new TIntArrayList() : null;
  }

  public int getSize() {
    return myNextId;
  }

  public void resetMethod() {
    myLinesInCurrentMethod = false;
    myLastFalseJump = null;
    myLastTrueJump = null;
    if (myJumps != null) myJumps.clear();
    if (mySwitches != null) mySwitches.clear();
  }

  public Jump getJump(Label jump) {
    if (myJumps == null) return null;
    return myJumps.get(jump);
  }

  public Switch getSwitch(Label label) {
    if (mySwitches == null) return null;
    return mySwitches.get(label);
  }

  public int getLineCount() {
    return myLines.size();
  }

  public TIntObjectHashMap<LineData> getLines() {
    return myLines;
  }

  public LineData getLineData(int line) {
    return myLines.get(line);
  }

  public void createLineData(int line, String methodNameAndDesc) {
    boolean ignoreSection = isIgnoreSection();
    if (ignoreSection && !KotlinUtils.isKotlinClass(this)) return;
    myLinesInCurrentMethod = true;
    if (ignoreSection) onIgnoredLine(line);
    if (!ignoreSection && myIgnoredLines != null) myIgnoredLines.remove(line);
    LineData lineData = myLines.get(line);
    if (lineData == null) {
      lineData = new LineData(line, methodNameAndDesc);
      myLines.put(line, lineData);
    }
    if (lineData.getId() == -1) {
      lineData.setId(incrementId());
    }
    if (line > myMaxSeenLine) myMaxSeenLine = line;
  }

  public boolean hasNoLinesInCurrentMethod() {
    return !myLinesInCurrentMethod;
  }

  public int getMaxSeenLine() {
    return myMaxSeenLine;
  }

  public void addJump(LineData lineData, Label trueLabel, Label falseLabel) {
    int index = lineData.jumpsCount();
    int line = lineData.getLineNumber();
    // the jump type is inverted as jump occurs if value is true
    Jump trueJump = new Jump(incrementId(), index, line, false);
    Jump falseJump = new Jump(incrementId(), index, line, true);
    myLastTrueJump = trueLabel;
    myLastFalseJump = falseLabel;

    if (myJumps == null) myJumps = new HashMap<Label, Jump>();
    myJumps.put(falseLabel, falseJump);
    myJumps.put(trueLabel, trueJump);

    JumpData jumpData = lineData.addJump(index);
    jumpData.setId(trueJump.getId(), trueJump.getType());
    jumpData.setId(falseJump.getId(), falseJump.getType());
  }

  public void addSwitch(LineData lineData, int[] keys, Label dflt, Label[] labels) {
    final int index = lineData.switchesCount();
    List<Switch> switches = rememberSwitchLabels(lineData.getLineNumber(), dflt, labels, index);
    SwitchData switchData = lineData.addSwitch(index, keys);
    setSwitchIds(switchData, switches);
  }

  public boolean isIgnoreSection() {
    return myIgnoreSection > 0;
  }

  /**
   * Set ignore flag. All the lines are ignored when this flag is enabled.
   */
  public void setIgnoreSection(boolean ignore) {
    if (ignore) {
      myIgnoreSection++;
    } else {
      myIgnoreSection--;
    }
  }

  public void removeLine(final int line) {
    myLines.remove(line);
    onIgnoredLine(line);
  }

  public TIntHashSet getIgnoredLines() {
    return myIgnoredLines;
  }

  public void removeLastJump() {
    if (myLastTrueJump == null) return;
    myJumps.remove(myLastFalseJump);
    Jump trueJump = myJumps.remove(myLastTrueJump);
    myLastTrueJump = null;
    myLastFalseJump = null;

    if (trueJump == null) return;
    LineData lineData = myLines.get(trueJump.getLine());
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
    LineData lineData = myLines.get(aSwitch.getLine());
    if (lineData == null) return;
    lineData.removeSwitch(lineData.switchesCount() - 1);
  }

  public TIntArrayList getInstructions() {
    return myInstructions;
  }

  public void addInstructions(int id, int instructions) {
    myInstructions.set(id, myInstructions.get(id) + instructions);
  }

  private int incrementId() {
    if (myInstructions != null) {
      while (myInstructions.size() <= myNextId) {
        myInstructions.add(0);
      }
    }
    return myNextId++;
  }

  private List<Switch> rememberSwitchLabels(final int line, final Label dflt, final Label[] labels, int switchIndex) {
    List<Switch> result = new ArrayList<Switch>();
    if (mySwitches == null) mySwitches = new HashMap<Label, Switch>();

    Switch aSwitch = new Switch(incrementId(), switchIndex, line, -1);
    result.add(aSwitch);
    mySwitches.put(dflt, aSwitch);

    for (int i = labels.length - 1; i >= 0; i--) {
      aSwitch = new Switch(incrementId(), switchIndex, line, i);
      result.add(aSwitch);
      mySwitches.put(labels[i], aSwitch);
    }

    return result;
  }

  private void onIgnoredLine(final int line) {
    if (myIgnoredLines == null) {
      myIgnoredLines = new TIntHashSet();
    }
    myIgnoredLines.add(line);
  }

  private void setSwitchIds(SwitchData data, List<Switch> switches) {
    for (Switch aSwitch : switches) {
      data.setId(aSwitch.getId(), aSwitch.getKey());
    }
  }

  public <T> void put(Key<T> key, T value) {
    myProperties.put(key, value);
  }

  @SuppressWarnings("unchecked")
  public <T> T get(Key<T> key) {
    return (T) myProperties.get(key);
  }

  public int getMethodAccess() {
    return get(Key.METHOD_ACCESS);
  }

  public String getMethodName() {
    return get(Key.METHOD_NAME);
  }

  public String getMethodDesc() {
    return get(Key.METHOD_DESC);
  }
}
