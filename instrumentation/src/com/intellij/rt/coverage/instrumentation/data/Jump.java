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

public class Jump {
  private final int myId;
  private final int myIndex;
  private final int myLine;
  private final boolean myType;

  public Jump(int id, int index, int line, boolean type) {
    myId = id;
    myIndex = index;
    myLine = line;
    myType = type;
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

  public boolean getType() {
    return myType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Jump jump = (Jump) o;

    return myIndex == jump.myIndex
        && myLine == jump.myLine
        && myType == jump.myType;
  }

  @Override
  public int hashCode() {
    int result = myIndex;
    result = 31 * result + myLine;
    result = 31 * result + (myType ? 1 : 0);
    return result;
  }
}
