/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

package testData.fixes.IDEA_332710;

import java.util.ArrayList;
import java.util.List;

// classes: Test
public class Test {  // coverage: NONE

  static class Entry {
    private final Object a, b;
    private final boolean x;

    Entry(Object a, Object b, boolean x) {
      this.a = a;
      this.b = b;
      this.x = x;
    }

    public boolean isX() {
      return x;
    }

    public Object getA() {
      return a;
    }

    public Object getB() {
      return b;
    }
  }

  public static void main(String[] args) {
    List<Entry> entryList = new ArrayList<Entry>(); // coverage: FULL
    entryList.add(new Entry(null, null, false)); // coverage: FULL
    entryList.add(new Entry(new Object(), null, true)); // coverage: FULL
    entryList.add(new Entry(null, new Object(), false)); // coverage: FULL
    entryList.add(new Entry(new Object(), new Object(), false)); // coverage: FULL
    entryList.add(new Entry(new Object(), new Object(), true)); // coverage: FULL

    for (Entry entry : entryList) { // coverage: FULL
      if (entry.getA() == null || // coverage: FULL
          entry.getB() == null) { // coverage: FULL
        continue; // coverage: FULL
      }
      if (!entry.isX()) { // coverage: FULL
        continue; // coverage: FULL
      }

      System.out.println(); // coverage: FULL
    } // coverage: FULL
  }
}
