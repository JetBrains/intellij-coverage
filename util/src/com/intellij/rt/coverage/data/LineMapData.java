/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

/**
 * For each source line <code>x</code> in <code>[mySourceStart, myCount]</code>, lines
 * <p>
 * <code>[myMappedStart + (x - mySourceStart) * myIncrement, myMappedStart + (x - mySourceStart + 1) * myIncrement)</code>
 * <p>
 * are mapped to line <code>x</code>.
 * <p>
 *
 * @author anna
 * @since 2/9/11
 */
public class LineMapData {
  private final int mySourceStart;
  private final int myCount;
  private final int myMappedStart;
  private final int myIncrement;

  public LineMapData(int sourceStart, int count, int mappedStart, int increment) {
    mySourceStart = sourceStart;
    myCount = count;
    myMappedStart = mappedStart;
    myIncrement = increment;
  }

  public int getCount() {
    return myCount;
  }

  public int getSourceLine(int index) {
    checkIndex(index);
    return mySourceStart + index;
  }

  public int getMappingStart(int index) {
    checkIndex(index);
    return myMappedStart + index * myIncrement;
  }

  public int getMappingEnd(int index) {
    checkIndex(index);
    return myMappedStart + (index + 1) * myIncrement;
  }

  public String toString() {
    return "map [" + myMappedStart + "; " + myMappedStart + myCount * myIncrement + ") to [" + mySourceStart + "; " + mySourceStart + myCount + ")";
  }

  private void checkIndex(int index) {
    if (index < 0 || index >= myCount) {
      throw new IllegalArgumentException("Invalid index " + index + " in mapping " + this);
    }
  }
}
