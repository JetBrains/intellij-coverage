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

package com.intellij.rt.coverage.verify.api;

import java.io.File;
import java.util.List;

public class Rule {
  public final int id;
  public final File reportFile;
  public final Target target;
  public final List<Bound> bounds;


  public Rule(int id, File reportFile, Target target, List<Bound> bounds) {
    this.id = id;
    this.reportFile = reportFile;
    this.target = target;
    this.bounds = bounds;
  }
}
