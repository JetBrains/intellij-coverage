package com.intellij.rt.coverage.data;

import com.intellij.rt.coverage.util.CoverageIOUtil;
import com.intellij.rt.coverage.util.DictionaryLookup;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pavel.Sher
 */
public class JumpsAndSwitches implements CoverageData {
  private List myJumps;
  private JumpData[] myJumpsArray;

  private List mySwitches;
  private SwitchData[] mySwitchesArray;

  public List getJumps() {
    return myJumps;
  }

  public List getSwitches() {
    return mySwitches;
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

  public SwitchData getSwitchData(int switchNumber) {
    return mySwitches == null && mySwitchesArray == null ? null : mySwitchesArray[switchNumber];
  }

  public void save(final DataOutputStream os, DictionaryLookup dictionaryLookup) throws IOException {
    CoverageIOUtil.writeINT(os, myJumpsArray != null ? myJumpsArray.length : 0);
    if (myJumpsArray != null) {
      for (int j = 0; j < myJumpsArray.length; j++) {
        myJumpsArray[j].save(os, dictionaryLookup);
      }
    }
    CoverageIOUtil.writeINT(os, mySwitchesArray != null ? mySwitchesArray.length : 0);
    if (mySwitchesArray != null) {
      for (int s = 0; s < mySwitchesArray.length; s++) {
        mySwitchesArray[s].save(os, dictionaryLookup);
      }
    }
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

  public void merge(final CoverageData data) {
    JumpsAndSwitches jumpsData = (JumpsAndSwitches)data;
    if (jumpsData.myJumps != null) {
      if (myJumps == null) {
        myJumps = jumpsData.myJumps;
      }
      else if (jumpsData.myJumps != null) {
        for (int i = Math.min(myJumps.size(), jumpsData.myJumps.size()) - 1; i >= 0; i--) {
          ((JumpData)myJumps.get(i)).merge((JumpData) jumpsData.myJumps.get(i));
        }
        for (int i = Math.min(myJumps.size(), jumpsData.myJumps.size()); i < jumpsData.myJumps.size(); i++) {
          myJumps.add(jumpsData.myJumps.get(i));
        }
      }
    }
    if (jumpsData.mySwitches != null) {
      if (mySwitches == null) {
        mySwitches = jumpsData.mySwitches;
      }
      else if (jumpsData.mySwitches != null) {
        for (int i = Math.min(mySwitches.size(), jumpsData.mySwitches.size()) - 1; i >= 0; i--) {
          ((SwitchData)mySwitches.get(i)).merge((SwitchData) jumpsData.mySwitches.get(i));
        }
        for (int i = Math.min(mySwitches.size(), jumpsData.mySwitches.size()); i < jumpsData.mySwitches.size(); i++) {
          mySwitches.add(jumpsData.mySwitches.get(i));
        }
      }
    }
  }
}
