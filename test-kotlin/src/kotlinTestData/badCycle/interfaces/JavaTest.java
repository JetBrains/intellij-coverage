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

package kotlinTestData.badCycle.interfaces;

public class JavaTest {
  public interface Interface {
    Printer x = new Printer("Interface static init start");
    int code = new ImplementerClass().foo();       // call to ImplementerInterface default method before static init
    Printer y = new Printer("Interface static init end");

    default void method() {
    }
  }

  public interface ImplementerInterface extends Interface {
    Printer x = new Printer("ImplementerInterface static init"); // coverage: FULL

    default int foo() {
      System.out.println("ImplementerInterface default method"); // coverage: FULL
      return 42;                                                 // coverage: FULL
    }
  }

  public static class ImplementerClass implements ImplementerInterface {
    static {
      System.out.println("ImplementerClass static init");
    }
  }

  static class Printer {
    Printer(String message) {
      System.out.println(message);
    }
  }

}
