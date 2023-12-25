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

package com.intellij.rt.coverage.offline;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A storage for class data at runtime in case of offline instrumentation
 */
public class RawProjectData {
  private final Map<String, RawClassData> myClasses = new ConcurrentHashMap<String, RawClassData>();

  public Collection<RawClassData> getClasses() {
    return myClasses.values();
  }

  public RawClassData getOrCreateClass(String className, int length, boolean hits) {
    final RawClassData classData = myClasses.get(className);
    if (classData != null) {
      checkLength(classData, length);
      return classData;
    }
    return createClassData(className, length, hits);
  }

  private synchronized RawClassData createClassData(String className, int length, boolean hits) {
    RawClassData classData = myClasses.get(className);
    if (classData != null) {
      checkLength(classData, length);
      return classData;
    }
    classData = new RawClassData(className, hits ? new int[length] : new boolean[length]);
    myClasses.put(className, classData);
    return classData;
  }

  private static void checkLength(RawClassData classData, int length) {
    if (classData.getLength() < length) {
      throw new RuntimeException("Class " + classData.name + " is loaded twice with different hits length");
    }
  }
}
