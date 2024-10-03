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

package testData.basicJava.tryWithResources.java;

import java.io.Closeable;
import java.io.IOException;

public class Test { // coverage: NONE
  static class AutoCloseableImpl implements Closeable { // coverage: FULL
    @Override
    public void close() {
    } // coverage: FULL
  }

  public static void main(String[] args) throws IOException {
    test2(); // coverage: FULL
    test3(); // coverage: FULL
    test4(); // coverage: FULL
    test5(); // coverage: FULL
  } // coverage: FULL

  private static void test2() throws IOException {
    try (
        Closeable x = new AutoCloseableImpl() // coverage: FULL
    ) {
      System.out.println("action"); // coverage: FULL
    }
    System.out.println(); // coverage: FULL
  } // coverage: FULL

  private static int test3() throws IOException {
    try (
        Closeable x = new AutoCloseableImpl(); // coverage: FULL
    ) {
      return 42; // coverage: FULL
    }
  }

  private static int test4() throws IOException {
    try (
        Closeable x = new AutoCloseableImpl() // coverage: FULL
    ) {
      return 42; // coverage: FULL
    } catch (Exception e) {
      throw new RuntimeException(e); // coverage: NONE
    }
  }

  private static int test5() throws IOException {
    try (
          Closeable y = new AutoCloseableImpl(); // coverage: FULL
          Closeable x = new AutoCloseableImpl() // coverage: FULL
    ) {
      return 42; // coverage: FULL
    }
  }
}
