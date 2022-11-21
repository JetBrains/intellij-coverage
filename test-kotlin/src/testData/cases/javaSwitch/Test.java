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

package testData.cases.javaSwitch;

public class Test { // coverage: NONE
  static int f(int x) {
    switch (x) {              // coverage: FULL
      case 1: return 42;      // coverage: FULL
      case 2: return 43;      // coverage: FULL
      default: return 52;     // coverage: FULL
    }
  }

  public static void main(String[] args) {
    Test.f(1);      // coverage: FULL
    Test.f(2);      // coverage: FULL
    Test.f(3);      // coverage: FULL
  }
}
