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

package com.intellij.rt.coverage.report;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.ProjectData;

import java.io.File;
import java.util.*;

public class SourceFileLocator extends FileLocator {
  private final Map<String, List<File>> mySourceFiles = new HashMap<String, List<File>>();

  public SourceFileLocator(List<File> roots, ProjectData projectData) {
    super(roots);
    locateProjectSourceFiles(projectData);
  }

  @Override
  public List<File> locate(String fqName) {
    final List<File> result = mySourceFiles.get(fqName);
    if (result != null) return result;
    return Collections.emptyList();
  }

  private void locateProjectSourceFiles(ProjectData projectData) {
    final Map<String, String> lostSources = new HashMap<String, String>();
    for (ClassData classData : projectData.getClasses().values()) {
      if (classData == null) continue;
      final String className = classData.getName();
      final String fileName = classData.getSource();
      if (fileName == null || className == null) continue;
      final int packageIndex = className.lastIndexOf('.');

      final List<File> candidates = locateFile(packageIndex < 0 ? "" : className.substring(0, packageIndex), fileName);
      if (!candidates.isEmpty()) {
        mySourceFiles.put(className, candidates);
      } else {
        lostSources.put(fileName, className);
      }
    }
    if (lostSources.isEmpty()) return;
    for (File root : myRoots) {
      final List<File> stack = new ArrayList<File>();
      stack.add(root);
      while (!stack.isEmpty()) {
        final File file = stack.remove(stack.size() - 1);
        if (file.isFile()) {
          final String className = lostSources.get(file.getName());
          if (className == null) continue;
          List<File> classSources = mySourceFiles.get(className);
          if (classSources == null) {
            classSources = new ArrayList<File>();
            mySourceFiles.put(className, classSources);
          }
          classSources.add(file);
          lostSources.remove(file.getName());
          if (lostSources.isEmpty()) return;
        } else if (file.isDirectory()) {
          final File[] children = file.listFiles();
          if (children != null) {
            stack.addAll(Arrays.asList(children));
          }
        }
      }
    }
  }
}
