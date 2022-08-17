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

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.report.util.FileLocator;
import com.intellij.rt.coverage.report.util.FileUtils;
import com.intellij.rt.coverage.report.util.SourceFileLocator;
import jetbrains.coverage.report.SourceCodeProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class DirectorySourceCodeProvider implements SourceCodeProvider {
  private final FileLocator myFileLocator;

  public DirectorySourceCodeProvider(ProjectData projectData, List<File> sources) {
    myFileLocator = new SourceFileLocator(sources, projectData);
  }

  private static String readText(File file) {
    try {
      return FileUtils.readAll(file);
    } catch (IOException e) {
      return null;
    }
  }

  /*
   * An example when a class may have several source code candidates.
   * This issue occurs due to Kotlin directory structure,
   * which states that source file location may not match its package.
   *
   * Let's assume class c.Test with source file test.kt and the following files structure:
   * - source root
   * | - a
   *   | - test.kt
   * | - b
   *   | - test.kt
   *
   * In this case we cannot distinguish a/test.kt and b/test.kt as candidates of source file for c.Test.
   * In order to find the true source file, we can search for 'package c' line in candidate files.
   */
  /**
   * This method tries to find source code for a class.
   * If there are several candidates, it checks if a candidate file contains <code>package</code> directive.
   *
   * @param className fully qualified class name
   * @return source code string for the class
   */
  @Nullable
  @Override
  public CharSequence getSourceCode(@NotNull String className) {
    final List<File> candidates = myFileLocator.locate(className);
    if (candidates.isEmpty()) return null;
    if (candidates.size() == 1) return readText(candidates.get(0));

    final int packageIndex = className.lastIndexOf('.');
    final String packageName = packageIndex == -1 ? ".*" : className.substring(0, packageIndex).replace(".", "\\.");
    final Pattern pattern = Pattern.compile("package +" + packageName);
    String lastCandidateText = null;
    for (File candidate : candidates) {
      final String text = readText(candidate);
      if (text == null) continue;
      lastCandidateText = text;
      final boolean matchFound = pattern.matcher(lastCandidateText).find();
      if (packageIndex != -1 == matchFound) return lastCandidateText;
    }
    return lastCandidateText;
  }
}
