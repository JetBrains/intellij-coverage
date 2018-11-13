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

package com.intellij.rt.coverage.testDiscovery;

import com.intellij.rt.coverage.data.ClassMetadata;
import com.intellij.rt.coverage.data.NameEnumerator;
import com.intellij.rt.coverage.data.TestDiscoveryDataListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeafTestDiscoveryDataListener implements TestDiscoveryDataListener {
  private final List<ClassMetadata> classMetadata = new ArrayList<ClassMetadata>();

  public void testFinished(String className, String methodName, Map<Integer, boolean[]> classToVisitedMethods, Map<Integer, int[][]> classToMethodNames, List<int[]> openedFiles) {
  }

  public void testsFinished() {
  }

  public void addMetadata(Map<String, String> metadata) {
  }

  public void addClassMetadata(List<ClassMetadata> metadata) {
    this.classMetadata.addAll(metadata);
  }

  public List<ClassMetadata> getClassMetadata() {
    return classMetadata;
  }

  public NameEnumerator getNameEnumerator() {
    return new NameEnumerator();
  }
}
