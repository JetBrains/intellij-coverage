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
import jetbrains.coverage.report.SourceCodeProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class DirectorySourceCodeProvider implements SourceCodeProvider {
  private final ProjectData myProjectData;
  private final List<File> mySources;

  public DirectorySourceCodeProvider(ProjectData projectData, List<File> sources) {
    myProjectData = projectData;
    mySources = sources;
  }

  private static String[] getPath(final String packageName, final String fileName) {
    final String[] parts = packageName.split("\\.");
    final String[] pathParts = new String[parts.length + 1];
    System.arraycopy(parts, 0, pathParts, 0, parts.length);
    pathParts[parts.length] = fileName;
    return pathParts;
  }

  private static CharSequence readText(File file) throws IOException {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(file));
      final StringBuilder result = new StringBuilder();
      String line = reader.readLine();
      while (line != null) {
        result.append(line).append('\n');
        line = reader.readLine();
      }
      return result;
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }

  private static String joinPath(String... parts) {
    if (parts.length == 0) return "";
    final StringBuilder builder = new StringBuilder();
    builder.append(parts[0]);
    for (int i = 1; i < parts.length; i++) {
      builder.append(File.separator);
      builder.append(parts[i]);
    }
    return builder.toString();
  }

  @Nullable
  @Override
  public CharSequence getSourceCode(@NotNull String className) {
    final ClassData classData = myProjectData.getClassData(className);
    if (classData == null) return null;
    final int packageIndex = className.lastIndexOf('.');
    if (packageIndex < 0) return null;
    final String packageName = className.substring(0, packageIndex);
    final String fileName = classData.getSource();
    if (fileName == null) return null;
    final String path = joinPath(getPath(packageName, fileName));
    for (File f : mySources) {
      final File candidate = new File(f, path);
      if (candidate.exists() && candidate.isFile()) {
        try {
          return readText(candidate);
        } catch (IOException ignored) {
        }
      }
    }
    return null;
  }
}
