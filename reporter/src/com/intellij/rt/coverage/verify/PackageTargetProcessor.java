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
import com.intellij.rt.coverage.report.XMLCoverageReport;

import java.util.List;
import java.util.Map;

public class PackageTargetProcessor implements TargetProcessor {

  @Override
  public void process(ProjectData projectData, Consumer consumer) {
    final Map<String, List<ClassData>> classesToPackages = XMLCoverageReport.mapClassesToPackages(projectData);

    for (Map.Entry<String, List<ClassData>> packageEntry : classesToPackages.entrySet()) {
      final Verifier.CollectedCoverage packageCoverage = new Verifier.CollectedCoverage();
      for (ClassData classData : packageEntry.getValue()) {
        final Verifier.CollectedCoverage coverage = ProjectTargetProcessor.collectClassCoverage(projectData, classData);
        packageCoverage.add(coverage);
      }
      String packageName = packageEntry.getKey();
      if (packageName.equals("")) packageName = "root";
      consumer.consume(packageName, packageCoverage);
    }
  }
}
