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

package com.intellij.rt.coverage.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.gnu.trove.TObjectIntHashMap;
import org.jetbrains.coverage.gnu.trove.TObjectIntProcedure;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class TestDiscoveryProjectDataTestAccessor {
  public static Collection<ClassMetadata> getClassMetaData() {
    return TestDiscoveryProjectData.getProjectData().classesToMetadata.values();
  }

  @NotNull
  public static Map<String, String[]> getClass2MethodNameMap() {
    TestDiscoveryProjectData projectData = TestDiscoveryProjectData.getProjectData();
    TIntObjectHashMap<String> namesMap = reverse(projectData.getMyNameEnumerator().getNamesMap());

    ConcurrentMap<Integer, int[]> classToMethodNames = projectData.getClassToMethodNames();

    Map<String, String[]> result = new HashMap<String, String[]>();
    for (Map.Entry<Integer, int[]> entry : classToMethodNames.entrySet()) {
      int[] methodIds = entry.getValue();
      String[] methodNames = new String[methodIds.length];
      for (int i = 0; i < methodIds.length; i++) {
        methodNames[i] = namesMap.get(methodIds[i]);
      }
      result.put(namesMap.get(entry.getKey()), methodNames);
    }
    return result;
  }

  @NotNull
  public static Map<String, boolean[]> getClass2UsedMethodsMap() {
    TestDiscoveryProjectData projectData = TestDiscoveryProjectData.getProjectData();
    TIntObjectHashMap<String> namesMap = reverse(projectData.getMyNameEnumerator().getNamesMap());

    ConcurrentMap<Integer, boolean[]> classToMethodNames = projectData.getClassToVisitedMethods();
    Map<String, boolean[]> result = new HashMap<String, boolean[]>();
    for (Map.Entry<Integer, boolean[]> entry : classToMethodNames.entrySet()) {
      result.put(namesMap.get(entry.getKey()), entry.getValue());
    }
    return result;
  }

  private static TIntObjectHashMap<String> reverse(TObjectIntHashMap<String> names) {
    final TIntObjectHashMap<String> result = new TIntObjectHashMap<String>();
    names.forEachEntry(new TObjectIntProcedure<String>() {
      public boolean execute(String s, int i) {
        result.put(i, s);
        return true;
      }
    });
    return result;
  }
}
