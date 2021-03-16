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

public class Switch {
  private final int myId;
  private final int myIndex;
  private final int myLine;
  private final int myKey;

  public Switch(int id, int index, int line, int key) {
    myId = id;
    myIndex = index;
    myLine = line;
    myKey = key;
  }

  public int getId() {
    return myId;
  }

  public int getIndex() {
    return myIndex;
  }

  public int getLine() {
    return myLine;
  }

  public int getKey() {
    return myKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Switch aSwitch = (Switch) o;

    return myIndex == aSwitch.myIndex
        && myLine == aSwitch.myLine
        && myKey == aSwitch.myKey;
  }

  @Override
  public int hashCode() {
    int result = myIndex;
    result = 31 * result + myLine;
    result = 31 * result + myKey;
    return result;
  }
}
