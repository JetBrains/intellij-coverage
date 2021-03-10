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

package kotlinTestData.badCycle.classes;

public class JavaTest {
  public static class BaseClass {                     // coverage: FULL
    static {
      System.out.println("Base class static init");   // coverage: FULL
      new DerivedClass().method();                    // coverage: FULL
    }                                                 // coverage: FULL
  }

  public static class DerivedClass extends BaseClass {
    public DerivedClass() {                           // coverage: FULL
      System.out.println("DerivedClass constructor"); // coverage: FULL
    }                                                 // coverage: FULL

    static {
      System.out.println("DerivedClass static init"); // coverage: FULL
    }                                                 // coverage: FULL

    void method() {
      System.out.println("DerivedClass method");      // coverage: FULL
    }                                                 // coverage: FULL
  }
}
