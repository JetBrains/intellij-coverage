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

package testData.cases.fallthrough;

// offline instrumentation

public class Test {                    // coverage: FULL
  public static void main(String[] args) {
    Test t = new Test();               // coverage: FULL
    t.testCovered(1);                  // coverage: FULL
    t.testCovered(2);                  // coverage: FULL
    t.testCovered(3);                  // coverage: FULL

    t.testUncovered1(1);               // coverage: FULL
    t.testUncovered1(3);               // coverage: FULL

    t.testUncovered2(2);               // coverage: FULL
    t.testUncovered2(3);               // coverage: FULL
  } // coverage: FULL

  public void testCovered(int x) {
    switch (x) {                       // coverage: FULL // branches: 2/2
      case 1:
      case 2:
        System.out.println("1 or 2");  // coverage: FULL
      default:
        System.out.println("default"); // coverage: FULL
    }
  } // coverage: FULL

  public void testUncovered1(int x) {
    switch (x) {                       // coverage: PARTIAL // branches: 1/2
      case 1:
      case 2:
        System.out.println("1 or 2");  // coverage: FULL
      default:
        System.out.println("default"); // coverage: FULL
    }
  } // coverage: FULL

  public void testUncovered2(int x) {
    switch (x) {                       // coverage: PARTIAL // branches: 1/2
      case 1:
      case 2:
        System.out.println("1 or 2");  // coverage: FULL
      default:
        System.out.println("default"); // coverage: FULL
    }
  } // coverage: FULL
}
