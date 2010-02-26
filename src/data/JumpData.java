package com.intellij.rt.coverage.data;

import com.intellij.rt.coverage.util.CoverageIOUtil;
import com.intellij.rt.coverage.util.DictionaryLookup;

import java.io.DataOutputStream;
import java.io.IOException;

public class JumpData implements CoverageData {
  private int myTrueHits;
  private int myFalseHits;

  public void touchTrueHit() {
    myTrueHits++;
  }

  public void touchFalseHit() {
    myFalseHits++;
  }

  public int getTrueHits() {
    return myTrueHits;
  }

  public int getFalseHits() {
    return myFalseHits;
  }

  public void save(final DataOutputStream os, DictionaryLookup dictionaryLookup) throws IOException {
    CoverageIOUtil.writeINT(os, myTrueHits);
    CoverageIOUtil.writeINT(os, myFalseHits);
  }

  public void merge(final CoverageData data) {
    final JumpData jumpData = (JumpData)data;
    myTrueHits += jumpData.myTrueHits;
    myFalseHits += jumpData.myFalseHits;
  }

  public void setTrueHits(final int trueHits) {
    myTrueHits = trueHits;
  }

  public void setFalseHits(final int falseHits) {
    myFalseHits = falseHits;
  }
}
