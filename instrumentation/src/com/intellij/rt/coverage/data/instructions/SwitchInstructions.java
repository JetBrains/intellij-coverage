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

import com.intellij.rt.coverage.util.ArrayUtil;

public class SwitchInstructions {
  private int myDefaultInstructions = 0;
  private int[] myInstructions;

  public SwitchInstructions(int size) {
    myInstructions = new int[size];
  }

  public int size() {
    return myInstructions.length;
  }

  public void setInstructions(int key, int instructions) {
    if (key == -1) {
      myDefaultInstructions = instructions;
    } else {
      if (key < 0 || key >= myInstructions.length) return;
      myInstructions[key] = instructions;
    }
  }

  public int getInstructions(int key) {
    if (key == -1) return myDefaultInstructions;
    if (key < 0 || key >= myInstructions.length) return 0;
    return myInstructions[key];
  }

  public void merge(SwitchInstructions other) {
    if (myInstructions.length < other.myInstructions.length) {
      myInstructions = ArrayUtil.copy(myInstructions, other.myInstructions.length);
    }
    myDefaultInstructions = Math.max(myDefaultInstructions, other.myDefaultInstructions);
    for (int i = 0; i < Math.min(myInstructions.length, other.myInstructions.length); i++) {
      myInstructions[i] = Math.max(myInstructions[i], other.myInstructions[i]);
    }
  }
}
