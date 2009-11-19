package com.intellij.rt.coverage.data;

import com.intellij.rt.coverage.util.CoverageIOUtil;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LineData implements CoverageData {
  private final int myLineNumber;
  private String myMethodSignature;

  private int myHits = 0;

  private List myJumps;
  private JumpData[] myJumpsArray;

  private List mySwitches;
  private SwitchData[] mySwitchesArray;

  private byte myStatus = -1;
  private String myUniqueTestName = null;
  private boolean myMayBeUnique = true;

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

  public int getStatus() {
    if (myStatus != -1) return myStatus;
    if (myHits == 0) {
      myStatus = LineCoverage.NONE;
      return myStatus;
    }
    if (myJumps != null) {
      for (Iterator it = myJumps.iterator(); it.hasNext();) {
        final JumpData jumpData = (JumpData)it.next();
        if ((jumpData.getFalseHits() > 0 ? 1 : 0) + (jumpData.getTrueHits() > 0 ? 1 : 0) < 2){
          myStatus = LineCoverage.PARTIAL;
          return myStatus;
        }
      }
    }
    if (mySwitches != null) {
      for (Iterator it = mySwitches.iterator(); it.hasNext();) {
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
      CoverageIOUtil.writeINT(os, myJumpsArray != null ? myJumpsArray.length : 0);
      if (myJumpsArray != null) {
        for (int j = 0; j < myJumpsArray.length; j++) {
          myJumpsArray[j].save(os);
        }
      }
      CoverageIOUtil.writeINT(os, mySwitchesArray != null ? mySwitchesArray.length : 0);
      if (mySwitchesArray != null) {
        for (int s = 0; s < mySwitchesArray.length; s++) {
          mySwitchesArray[s].save(os);
        }
      }
    }
  }

  public void merge(final CoverageData data) {
    LineData lineData = (LineData)data;
    myHits += lineData.myHits;
    if (lineData.myJumps != null) {
      if (myJumps == null) {
        myJumps = lineData.myJumps;
      }
      else if (lineData.myJumps != null) {
        for (int i = Math.min(myJumps.size(), lineData.myJumps.size()) - 1; i >= 0; i--) {
          ((JumpData)myJumps.get(i)).merge((JumpData)lineData.myJumps.get(i));
        }
        for (int i = Math.min(myJumps.size(), lineData.myJumps.size()); i < lineData.myJumps.size(); i++) {
          myJumps.add(lineData.myJumps.get(i));
        }
      }
    }
    if (lineData.mySwitches != null) {
      if (mySwitches == null) {
        mySwitches = lineData.mySwitches;
      }
      else if (lineData.mySwitches != null) {
        for (int i = Math.min(mySwitches.size(), lineData.mySwitches.size()) - 1; i >= 0; i--) {
          ((SwitchData)mySwitches.get(i)).merge((SwitchData)lineData.mySwitches.get(i));
        }
        for (int i = Math.min(mySwitches.size(), lineData.mySwitches.size()); i < lineData.mySwitches.size(); i++) {
          mySwitches.add(lineData.mySwitches.get(i));
        }
      }
    }
    if (lineData.myMethodSignature != null) myMethodSignature = lineData.myMethodSignature;
  }

  public JumpData addJump(final int jump) {
    if (myJumps == null) myJumps = new ArrayList();
    if (myJumps.size() <= jump) {
      for (int i = myJumps.size(); i <= jump; i++){
        myJumps.add(new JumpData());
      }
    }
    return (JumpData)myJumps.get(jump);
  }

  public JumpData getJumpData(int jump) {
    return myJumps == null && myJumpsArray == null ? null : myJumpsArray[jump];
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
    if (mySwitches == null) mySwitches = new ArrayList();
    final SwitchData switchData = new SwitchData(keys);
    if (mySwitches.size() <= switchNumber) {
      for(int i = mySwitches.size(); i < switchNumber; i++) {
        mySwitches.add(new SwitchData(new int[0]));
      }
      if (mySwitches.size() == switchNumber) {
        mySwitches.add(switchData);
      }
    }
    return (SwitchData)mySwitches.get(switchNumber);
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

  public SwitchData getSwitchData(int switchNumber) {
    return mySwitches == null && mySwitchesArray == null ? null : mySwitchesArray[switchNumber];
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
    return myJumps;
  }

  public List getSwitches() {
    return mySwitches;
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
    if (jump > 0 && jump <= myJumps.size()) {
      myJumps.remove(jump - 1);
    }
  }

  public void fillArrays() {
    if (myJumps != null) {
      myJumpsArray = new JumpData[myJumps.size()];
      for (int i = 0; i < myJumps.size(); i++) {
        myJumpsArray[i] = (JumpData)myJumps.get(i);
      }
      myJumps = null;
    }
    if (mySwitches != null) {
      mySwitchesArray = new SwitchData[mySwitches.size()];
      for (int i = 0; i < mySwitches.size(); i++) {
        mySwitchesArray[i] = (SwitchData)mySwitches.get(i);
      }
      mySwitches = null;
    }
  }
}
