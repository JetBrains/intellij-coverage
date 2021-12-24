/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package com.intellij.rt.coverage.report.data;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class Module {
  private final List<File> myOutputRoots;
  private final List<File> mySourceRoots;

  public Module(List<File> outputRoots, List<File> sourceRoots) {
    myOutputRoots = outputRoots;
    mySourceRoots = sourceRoots;
  }

  public List<File> getSources() {
    if (mySourceRoots == null) return Collections.emptyList();
    return mySourceRoots;
  }

  public List<File> getOutputRoots() {
    return myOutputRoots;
  }
}
