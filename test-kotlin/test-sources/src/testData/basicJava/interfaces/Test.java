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

package testData.basicJava.interfaces;

public class Test {  // coverage: NONE

  interface InterfaceWithoutClinit {
    int foo();
  }

  interface InterfaceWithClinit {
    int x = getInt();  // coverage: FULL

    int foo();
  }

  public static int getInt() {
    return 42;  // coverage: FULL
  }

  public static void main(String[] args) throws Exception {
    Class.forName("testData.basicJava.interfaces.Test$InterfaceWithoutClinit"); // coverage: FULL
    Class.forName("testData.basicJava.interfaces.Test$InterfaceWithClinit");  // coverage: FULL
  } // coverage: FULL
}
