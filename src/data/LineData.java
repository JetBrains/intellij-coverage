package com.intellij.rt.coverage.data;

import com.intellij.rt.coverage.util.CoverageIOUtil;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class LineData implements CoverageData {
  private final int myLineNumber;
  private String myMethodSignature;

  private int myHits = 0;

  private byte myStatus = -1;
  private String myUniqueTestName = null;
  private boolean myMayBeUnique = true;

  private JumpsAndSwitches myJumpsAndSwitches;

  public LineData(final int line, final String desc) {
    myLineNumber = line;
    myMethodSignature = desc;
  }

  public void touch() {
    myHits++;
    setTestName(ProjectData.getProjectData().getCurrentTestName());
  }

  public int getHits() {
    return myHits;
  }

  JumpsAndSwitches getJumpsAndSwitches() {
    if (myJumpsAndSwitches == null) {
      myJumpsAndSwitches = new JumpsAndSwitches();
    }
    return myJumpsAndSwitches;
  }

  public int getStatus() {
    if (myStatus != -1) return myStatus;
    if (myHits == 0) {
      myStatus = LineCoverage.NONE;
      return myStatus;
    }

    List jumps = getJumpsAndSwitches().getJumps();
    if (jumps != null) {
      for (Iterator it = jumps.iterator(); it.hasNext();) {
        final JumpData jumpData = (JumpData)it.next();
        if ((jumpData.getFalseHits() > 0 ? 1 : 0) + (jumpData.getTrueHits() > 0 ? 1 : 0) < 2){
          myStatus = LineCoverage.PARTIAL;
          return myStatus;
        }
      }
    }

    List switches = getJumpsAndSwitches().getSwitches();
    if (switches != null) {
      for (Iterator it = switches.iterator(); it.hasNext();) {
        final SwitchData switchData = (SwitchData)it.next();
        if (switchData.getDefaultHits() == 0){
          myStatus = LineCoverage.PARTIAL;
          return myStatus;
        }
        for (int i = 0; i < switchData.getHits().length; i++) {
          int hit = switchData.getHits()[i];
          if (hit == 0){
            myStatus = LineCoverage.PARTIAL;
            return myStatus;
          }
        }
      }
    }
    myStatus = LineCoverage.FULL;
    return myStatus;
  }

  public void save(final DataOutputStream os) throws IOException {
    CoverageIOUtil.writeINT(os, myLineNumber);
    CoverageIOUtil.writeUTF(os, myUniqueTestName != null ? myUniqueTestName : "");
    CoverageIOUtil.writeINT(os, myHits);
    if (myHits > 0) {
      getJumpsAndSwitches().save(os);
    }
  }

  public void merge(final CoverageData data) {
    LineData lineData = (LineData)data;
    myHits += lineData.myHits;
    getJumpsAndSwitches().merge(lineData.getJumpsAndSwitches());
    if (lineData.myMethodSignature != null) myMethodSignature = lineData.myMethodSignature;
  }

  public JumpData addJump(final int jump) {
    return getJumpsAndSwitches().addJump(jump);
  }

  public JumpData getJumpData(int jump) {
    return getJumpsAndSwitches().getJumpData(jump);
  }

  public void touchBrunch(final int jump, final boolean hit) {
    final JumpData jumpData = getJumpData(jump);
    if (jumpData != null) {
      if (hit) {
        jumpData.touchTrueHit();
      }
      else {
        jumpData.touchFalseHit();
      }
    }
  }

  public SwitchData addSwitch(final int switchNumber, final int[] keys) {
    return getJumpsAndSwitches().addSwitch(switchNumber, keys);
  }

  public SwitchData getSwitchData(int switchNumber) {
    return getJumpsAndSwitches().getSwitchData(switchNumber);
  }

  public SwitchData addSwitch(final int switchNumber, final int min, final int max) {
    int[] keys = new int[max - min + 1];
    for (int i = min; i <= max; i++) {
      keys[i - min] = i;
    }
    return addSwitch(switchNumber, keys);
  }

  public void touchBrunch(final int switchNumber, final int key) {
    final SwitchData switchData = getSwitchData(switchNumber);
    if (switchData != null) {
      switchData.touch(key);
    }
  }

  public int getLineNumber() {
    return myLineNumber;
  }

  public String getMethodSignature() {
    return myMethodSignature;
  }

  public void setStatus(final byte status) {
    myStatus = status;
  }

  public void setTrueHits(final int jumpNumber, final int trueHits) {
    addJump(jumpNumber).setTrueHits(trueHits);
  }

  public void setFalseHits(final int jumpNumber, final int falseHits) {
    addJump(jumpNumber).setFalseHits(falseHits);
  }

  public void setDefaultHits(final int switchNumber, final int[] keys, final int defaultHit) {
    addSwitch(switchNumber, keys).setDefaultHits(defaultHit);
  }

  public void setSwitchHits(final int switchNumber, final int[] keys, final int[] hits) {
    addSwitch(switchNumber, keys).setKeysAndHits(keys, hits);
  }

  public List getJumps() {
    return getJumpsAndSwitches().getJumps();
  }

  public List getSwitches() {
    return getJumpsAndSwitches().getSwitches();
  }

  public void setHits(final int hits) {
    myHits = hits;
  }

  public void setTestName(String testName) {
    if (testName != null) {
      if (myUniqueTestName == null) {
        if (myMayBeUnique) myUniqueTestName = testName;
      } else if (!myUniqueTestName.equals(testName)) {
        myUniqueTestName = null;
        myMayBeUnique = false;
      }
    }
  }

  public boolean isCoveredByOneTest() {
    return myUniqueTestName != null && myUniqueTestName.length() > 0;
  }

  public void removeJump(final int jump) {
    getJumpsAndSwitches().removeJump(jump);
  }

  public void fillArrays() {
    getJumpsAndSwitches().fillArrays();
  }
}
