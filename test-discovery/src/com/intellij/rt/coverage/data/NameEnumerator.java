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

import org.jetbrains.coverage.gnu.trove.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.List;

public class NameEnumerator {
  private final TObjectIntHashMap<String> myNames = new TObjectIntHashMap<String>();
  private int myNextNameId = 1; // because TObjectIntHashMap uses 0 as null
  private final Object myNameLock = "NameLock";

  int enumerate(String name) {
    synchronized (myNameLock) {
      int enumerated = myNames.get(name);
      if (enumerated != 0) return enumerated;

      int newId = myNextNameId++;
      updateDataIncrement(name, newId);
      myNames.put(name, newId);
      return newId;
    }
  }

  TObjectIntHashMap<String> getNamesMap() {
    return myNames;
  }

  protected void updateDataIncrement(String name, int id) {
  }

  static class Incremental extends NameEnumerator {
    private List<NameAndId> myDataIncrement = new ArrayList<NameAndId>();
    private final Object myDataIncrementLock = "DataIncrementLock";

    protected void updateDataIncrement(String name, int id) {
      synchronized (myDataIncrementLock) {
        myDataIncrement.add(new NameAndId(name, id));
      }
    }

    List<NameAndId> getAndClearDataIncrement() {
      synchronized (myDataIncrementLock) {
        List<NameAndId> dataIncrement = myDataIncrement;
        myDataIncrement = new ArrayList<NameAndId>();
        return dataIncrement;
      }
    }

    static final class NameAndId {
      private final String myName;
      private final int myId;

      NameAndId(String myName, int myId) {
        this.myName = myName;
        this.myId = myId;
      }

      String getName() {
        return myName;
      }

      int getId() {
        return myId;
      }
    }
  }
}
