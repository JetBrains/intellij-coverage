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

package testData.basicJava.assertion;

// extra args: -ea
// instructions & branches

public class Test { // coverage: FULL // stats: 6/6

  static void doAssert(boolean x) {
    assert x; // coverage: FULL // branches: 2/2 // stats: 8/8
  }

  public static void main(String[] args) {
    doAssert(true); // coverage: FULL // stats: 2/2
    try {
      doAssert(false); // coverage: FULL // stats: 2/2
    } catch (AssertionError ignore) { // coverage: FULL // stats: 1/1
    }
  }
}
