/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

package kotlinTestData.defaultInterfaceMember.java;

public class Test {
  public static void main(String[] args) {
    new Bar().foo2();
  }
}


interface Foo {
  default void foo1() {
    System.out.println();  // coverage: NONE
  }

  default void foo2() {
    System.out.println();  // coverage: FULL
  }

  default void foo3() {
    System.out.println();  // coverage: NONE
  }
}

class Bar implements Foo {
  public Bar() { }         // coverage: FULL

  @Override
  public void foo1() {
    System.out.println();  // coverage: NONE
  }

}
