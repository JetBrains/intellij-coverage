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
import jetbrains.coverage.report.SourceCodeProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class DirectorySourceCodeProvider implements SourceCodeProvider {
  private final FileLocator myFileLocator;

  public DirectorySourceCodeProvider(ProjectData projectData, List<File> sources) {
    myFileLocator = new SourceFileLocator(sources, projectData);
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

  @Nullable
  @Override
  public CharSequence getSourceCode(@NotNull String className) {
    for (File candidate : myFileLocator.locate(className)) {
      try {
        return readText(candidate);
      } catch (IOException ignored) {
      }
    }
    return null;
  }
}
