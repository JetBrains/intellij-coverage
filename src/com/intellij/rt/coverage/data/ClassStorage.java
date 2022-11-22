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

package com.intellij.rt.coverage.data;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

class ClassStorage {
  private final Map<String, ClassData> myMap = new ConcurrentHashMap<String, ClassData>(256);
  private volatile ClassData[] myArray = new ClassData[256];
  private final AtomicInteger myCounter = new AtomicInteger(0);

  public ClassData get(String name) {
    return myMap.get(name);
  }

  public ClassData getOrCreate(String name) {
    ClassData classData = myMap.get(name);
    if (classData != null) return classData;
    synchronized (this) {
      classData = myMap.get(name);
      if (classData != null) return classData;
      return createClassData(name);
    }
  }

  public ClassData get(int id) {
    return myArray[id];
  }

  public Collection<ClassData> values() {
    return myMap.values();
  }

  public Map<String, ClassData> map() {
    return myMap;
  }

  public int size() {
    return myMap.size();
  }

  private ClassData createClassData(String name) {
    final int id = myCounter.getAndIncrement();
    final ClassData classData = new ClassData(name, id);

    if (id >= myArray.length) {
      final ClassData[] newArray = new ClassData[myArray.length * 2];
      System.arraycopy(myArray, 0, newArray, 0, myArray.length);
      myArray = newArray;
    }
    myArray[id] = classData;

    myMap.put(name, classData);
    return classData;
  }
}
