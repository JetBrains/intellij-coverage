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

package kotlinTestData.fixes.IDEA_259332;

public class SwitchWithFallthrough {    // coverage: FULL
  public void testCovered(int x) {
    switch (x) {                        // coverage: FULL
      case 1:
      case 2:
        System.out.println("1 or 2");   // coverage: FULL
      default:
        System.out.println("default");  // coverage: FULL
    }
  }

  public void testUncovered1(int x) {
    switch (x) {                        // coverage: PARTIAL
      case 1:
      case 2:
        System.out.println("1 or 2");   // coverage: FULL
      default:
        System.out.println("default");  // coverage: FULL
    }
  }

  public void testUncovered2(int x) {
    switch (x) {                        // coverage: PARTIAL
      case 1:
      case 2:
        System.out.println("1 or 2");   // coverage: FULL
      default:
        System.out.println("default");  // coverage: FULL
    }
  }
}
