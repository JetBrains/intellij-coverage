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

package com.intellij.rt.coverage.util;

public class ArrayUtil {

  public static <T> T safeLoad(T[] array, int index) {
    return array != null && 0 <= index && index < array.length ? array[index] : null;
  }

  public static <T> void safeStore(T[] array, int index, T element) {
    if (array != null && 0 <= index && index < array.length) {
      array[index] = element;
    }
  }

  public static int[] copy(int[] array) {
    return copy(array, array.length);
  }

  public static int[] copy(int[] array, int newLength) {
    int[] result = new int[newLength];
    System.arraycopy(array, 0, result, 0, Math.min(array.length, newLength));
    return result;
  }

  public static boolean[] copy(boolean[] array) {
    return copy(array, array.length);
  }

  public static boolean[] copy(boolean[] array, int newLength) {
    boolean[] result = new boolean[newLength];
    System.arraycopy(array, 0, result, 0, Math.min(array.length, newLength));
    return result;
  }
}
