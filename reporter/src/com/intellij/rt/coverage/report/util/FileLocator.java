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

package com.intellij.rt.coverage.report.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class FileLocator {
  protected final List<File> myRoots;

  public FileLocator(List<File> roots) {
    myRoots = roots;
  }

  public abstract List<File> locate(String fqName);

  protected List<File> locateFile(String packageName, String fileName) {
    final String path = getPath(packageName, fileName);
    final List<File> result = new ArrayList<File>();
    for (File f : myRoots) {
      final File candidate = new File(f, path);
      if (candidate.exists() && candidate.isFile()) {
        result.add(candidate);
      }
    }
    return result;
  }

  private static String getPath(final String packageName, final String name) {
    final String[] parts = packageName.split("\\.");
    final StringBuilder builder = new StringBuilder();
    for (String part : parts) {
      builder.append(part);
      builder.append(File.separator);
    }
    builder.append(name);
    return builder.toString();
  }
}
