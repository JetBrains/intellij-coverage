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

package testData.cases.elseif;

// offline instrumentation

public class Test {                          // coverage: FULL
  public static void main(String[] args) {
    Test test = new Test();                  // coverage: FULL
    test.doTest(1, 2, 2);                    // coverage: FULL
    test.doTest(2, 2, 3);                    // coverage: FULL
    test.doTest(2, 2, 1);                    // coverage: FULL
  }

  void doTest(int a, int b, int c) {
    if (a < b) {                             // coverage: FULL // branches: 2/2
      System.out.println("a < b");           // coverage: FULL
    }
    else if (b < c) {                        // coverage: FULL // branches: 2/2
      System.out.println("a >= b && b < c"); // coverage: FULL
    }
  }
}
