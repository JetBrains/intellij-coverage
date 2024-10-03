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

package testData.basicJava.tryFinally.java;

public class Test {              // coverage: NONE
  static void test(Runnable runnable) {
    try {
      runnable.run();                // coverage: FULL
    } finally {
      System.out.println("Finally"); // coverage: FULL
    }
  } // coverage: FULL

  static int testWithReturn(Runnable runnable) {
    try {
      runnable.run();                // coverage: FULL
      return 42;                     // coverage: FULL
    } finally {
      System.out.println("Finally"); // coverage: FULL
    }
  }

  static void testWithCatch(Runnable runnable) {
    try {
      runnable.run();                 // coverage: FULL
    } catch (RuntimeException e) {
      System.out.println("Error");   // coverage: NONE
    } finally {
      System.out.println("Finally"); // coverage: FULL
    }
  } // coverage: FULL

  public static void main(String[] args) {
    test(() -> {});           // coverage: FULL
    testWithReturn(() -> {}); // coverage: FULL
    testWithCatch(() -> {});  // coverage: FULL
  } // coverage: FULL
}
