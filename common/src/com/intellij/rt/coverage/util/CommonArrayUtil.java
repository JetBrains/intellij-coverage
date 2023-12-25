/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

package com.intellij.rt.coverage.util;

public class CommonArrayUtil {
  /**
   * Transform int/boolean array to int array.
   * @param object must be an int or boolean array.
   * @return same int array or creates new array where all true values are replaced with 1
   */
  public static int[] getIntArray(Object object) {
    if (object == null) return null;
    if (object instanceof int[]) return (int[]) object;
    if (object instanceof boolean[]) {
      boolean[] mask = (boolean[]) object;
      int[] hits = new int[mask.length];
      for (int i = 0; i < mask.length; i++) {
        if (mask[i]) hits[i] = 1;
      }
      return hits;
    }
    throw new IllegalStateException("Unexpected type " + object.getClass().getName());
  }
}
