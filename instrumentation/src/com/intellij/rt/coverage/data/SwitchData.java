/*
 * Copyright 2000-2024 JetBrains s.r.o.
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

package com.intellij.rt.coverage.data;

import com.intellij.rt.coverage.util.ArrayUtil;
import com.intellij.rt.coverage.util.CoverageIOUtil;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class SwitchData implements CoverageData {
  private int[] myKeys;

  private int myDefaultHits;
  private int[] myHits;

  private int[] myIds;

  public SwitchData(final int[] keys) {

    myKeys = keys;

    myHits = new int[keys.length];

  }

  public void touch(final int key) {
    if (key == -1) {
      myDefaultHits++;
    } else if (key < myHits.length && key >= 0) {
      myHits[key]++;
    }
  }

  public int getDefaultHits() {
    return myDefaultHits;
  }

  public int[] getHits() {
    return myHits;
  }

  public void save(final DataOutputStream os) throws IOException {
    CoverageIOUtil.writeINT(os, myDefaultHits);
    CoverageIOUtil.writeINT(os, myHits.length);
    for (int i = 0; i < myHits.length; i++) {
      CoverageIOUtil.writeINT(os, myKeys[i]);
      CoverageIOUtil.writeINT(os, myHits[i]);
    }
  }

  public void merge(final CoverageData data) {
    SwitchData switchData = (SwitchData) data;
    setDefaultHits(myDefaultHits + switchData.myDefaultHits);
    if (switchData.myHits.length > myHits.length) {
      myHits = ArrayUtil.copy(myHits, switchData.myHits.length);
      myKeys = switchData.myKeys;
    }
    for (int i = Math.min(myHits.length, switchData.myHits.length) - 1; i >= 0; i--) {
      myHits[i] = ClassData.trimHits(myHits[i] + switchData.myHits[i]);
    }
    if (switchData.myIds != null) {
      if (myIds == null) {
        myIds = new int[myKeys.length + 1];
      }
      if (myIds.length < switchData.myIds.length) {
        myIds = ArrayUtil.copy(myIds, switchData.myIds.length);
      }
      for (int i = 0; i < switchData.myIds.length; i++) {
        if (switchData.myIds[i] != -1) {
          myIds[i] = switchData.myIds[i];
        }
      }
    }
  }

  public void setDefaultHits(final int defaultHits) {
    myDefaultHits = ClassData.trimHits(defaultHits);
  }

  public void setKeysAndHits(final int[] keys, final int[] hits) {
    myKeys = keys;
    myHits = hits;
    for (int i = 0; i < myHits.length; i++) {
      myHits[i] = ClassData.trimHits(myHits[i]);
    }
  }

  public int[] getKeys() {
    return myKeys;
  }

  public int getId(int index) {
    if (myIds == null) return -1;
    if (index == -1) return myIds[myIds.length - 1];
    if (index < 0 || index >= myIds.length) return -1;
    return myIds[index];
  }

  /**
   * Switch ID is used to store coverage data in an array at runtime.
   */
  public void setId(int id, int index) {
    if (myIds == null) {
      myIds = new int[myKeys.length + 1];
      Arrays.fill(myIds, -1);
    }
    if (index == -1) {
      myIds[myIds.length - 1] = id;
      return;
    }
    if (index < 0 || index >= myIds.length) {
      throw new ArrayIndexOutOfBoundsException("Incorrect index " + index + " length is " + myIds.length);
    }
    myIds[index] = id;
  }
}
