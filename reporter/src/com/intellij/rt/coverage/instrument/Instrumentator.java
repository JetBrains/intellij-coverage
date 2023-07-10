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

package com.intellij.rt.coverage.instrument;

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.CoverageTransformer;
import com.intellij.rt.coverage.report.api.Filters;
import com.intellij.rt.coverage.util.ClassNameUtil;
import com.intellij.rt.coverage.util.OptionsUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Perform offline instrumentation of class files in specified output roots.
 * If a class is not included into coverage instrumentation via filters,
 * its class file is copied to new output directory without modifications.
 */
public class Instrumentator {
  private final List<File> myRoots;
  private final List<File> myOutputRoots;
  private final Filters myFilters;


  public Instrumentator(List<File> roots, List<File> outputRoots, Filters filters) {
    myRoots = roots;
    myOutputRoots = outputRoots;
    myFilters = filters;
  }

  public void instrument(boolean countHits) {
    for (int i = 0; i < myRoots.size(); i++) {
      final File root = myRoots.get(i);
      final File outputRoot = myOutputRoots.get(i);

      final boolean newBranchCoverage = OptionsUtil.NEW_BRANCH_COVERAGE_ENABLED;
      final boolean calculateHitsCount = OptionsUtil.CALCULATE_HITS_COUNT;
      try {
        OptionsUtil.NEW_BRANCH_COVERAGE_ENABLED = true;
        OptionsUtil.CALCULATE_HITS_COUNT = countHits;
        new InstrumentationVisitor(root, outputRoot).visitFiles();
      } finally {
        OptionsUtil.NEW_BRANCH_COVERAGE_ENABLED = newBranchCoverage;
        OptionsUtil.CALCULATE_HITS_COUNT = calculateHitsCount;
      }
    }
  }

  private class InstrumentationVisitor extends DirectoryVisitor {
    private final File myOutput;
    private final CoverageTransformer myTransformer;

    private InstrumentationVisitor(File root, File output) {
      super(root);
      myOutput = output;
      final ProjectData projectData = ProjectData.createProjectData(true);
      projectData.setAnnotationsToIgnore(myFilters.excludeAnnotations);
      myTransformer = new OfflineCoverageTransformer(projectData, false, myFilters.excludeClasses, myFilters.includeClasses);
    }

    @Override
    protected void visitFile(String packageName, File file) {
      try {
        byte[] bytes = IOUtil.readBytes(file);

        if (file.getName().endsWith(ClassNameUtil.CLASS_FILE_SUFFIX)) {
          final String classSimpleName = ClassNameUtil.removeClassSuffix(file.getName());
          final String className = packageName.isEmpty()
              ? classSimpleName
              : ClassNameUtil.convertToInternalName(packageName) + "/" + classSimpleName;
          // This loader is not user actually, just need some not null loader
          final ClassLoader loader = ClassLoader.getSystemClassLoader();
          final byte[] transformed = myTransformer.transform(loader, className, null, null, bytes);
          if (transformed != null) {
            bytes = transformed;
          }
        }

        final File directory = new File(myOutput, packageName.replace(".", File.separator));
        if (!directory.exists() && !directory.mkdirs()) {
          throw new RuntimeException("Failed to create directory at " + directory.getAbsolutePath());
        }

        final File newFile = new File(directory, file.getName());
        IOUtil.writeBytes(newFile, bytes);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
