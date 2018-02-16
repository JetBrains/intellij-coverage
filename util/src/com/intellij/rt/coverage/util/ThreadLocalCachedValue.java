/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

import java.lang.ref.SoftReference;

abstract class ThreadLocalCachedValue<T> {
  private final ThreadLocal<SoftReference<T>> myThreadLocal = new ThreadLocal<SoftReference<T>>();

  public T getValue() {
    T value = dereference(myThreadLocal.get());
    if (value == null) {
      value = create();
      myThreadLocal.set(new SoftReference<T>(value));
    }
    return value;
  }

  protected abstract T create();

  private static <T> T dereference(SoftReference<T> ref) {
    return ref == null ? null : ref.get();
  }
}