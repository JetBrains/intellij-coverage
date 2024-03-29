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

package com.intellij.rt.coverage.verify;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.ProjectData;

/**
 * Calculates coverage summary for each class.
 */
public class ClassTargetProcessor implements TargetProcessor {

  @Override
  public void process(ProjectData projectData, Consumer consumer) {
    for (ClassData classData : projectData.getClassesCollection()) {
      final Verifier.CollectedCoverage coverage = ProjectTargetProcessor.collectClassCoverage(projectData, classData);
      consumer.consume(classData.getName(), coverage);
    }
  }
}
