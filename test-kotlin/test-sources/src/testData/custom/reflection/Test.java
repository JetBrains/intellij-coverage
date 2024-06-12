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

package testData.custom.reflection;

import java.lang.reflect.Field;

// classes: Test

public class Test {  // coverage: NONE
  public static class A {
    public static int x = 1;
    public int y = 2;
  }

  public static void main(String[] args) throws IllegalAccessException {
    final A instance = new A();                         // coverage: FULL
    final Field[] fields = A.class.getDeclaredFields(); // coverage: FULL
    for (Field field : fields) {                        // coverage: FULL // branches: 2/2
      field.setAccessible(true);                        // coverage: FULL
      System.out.println(field.getInt(instance));       // coverage: FULL
    }
  }
}
