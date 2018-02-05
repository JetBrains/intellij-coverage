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

import org.jetbrains.coverage.gnu.trove.THashMap;
import org.jetbrains.coverage.gnu.trove.TObjectIntHashMap;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IncrementalNameEnumerator {
  private static final boolean USE_DIRECT_BYTE_BUFFER = Boolean.valueOf(System.getProperty("incremental.name.enumerator.direct.byte.buffer", Boolean.TRUE.toString()));

  private final TObjectIntHashMap<String> myNames = new TObjectIntHashMap<String>();
  private List<NameAndId> myDataIncrement = new ArrayList<NameAndId>();
  private int myNextNameId = 1; // because of 0

  private final Object myNameLock = "NameLock";
  private final Object myDataIncrementLock = "DataIncrementLock";

  public int enumerate(String name) {
    synchronized (myNameLock) {
      int enumerated = myNames.get(name);
      if (enumerated != 0) return enumerated;

      int newId = myNextNameId++;
      updateDataIncrement(name, newId);
      myNames.put(name, newId);
      return newId;
    }
  }

  public List<NameAndId> getAndClearDataIncrement() {
    synchronized (myDataIncrementLock) {
      List<NameAndId> dataIncrement = myDataIncrement;
      myDataIncrement = new ArrayList<NameAndId>();
      return dataIncrement;
    }
  }

  private void updateDataIncrement(String name, ByteBuffer idx) {
    synchronized (myDataIncrementLock) {
      myDataIncrement.add(new NameAndId(name, idx));
    }
  }

  private static ByteBuffer toByteBuffer(int num) {
    ByteBuffer byteBuffer = USE_DIRECT_BYTE_BUFFER ? ByteBuffer.allocateDirect(4) : ByteBuffer.allocate(4);
    byteBuffer.putInt(num);
    return byteBuffer;
  }

  private static List<NameAndId> createEmptyDataIncrement() {
    return new ArrayList<NameAndId>(10);
  }

  public static final class NameAndId {
    private final String myName;
    private final ByteBuffer myId;

    NameAndId(String myName, ByteBuffer myId) {
      this.myName = myName;
      this.myId = myId;
    }

    public String getMyName() {
      return myName;
    }

    public ByteBuffer getMyId() {
      return myId;
    }
  }
}
