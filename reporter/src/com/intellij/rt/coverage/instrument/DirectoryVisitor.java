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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Visits all files in a directory.
 */
public class DirectoryVisitor {
  private final File myRoot;
  private final List<PackageDirectory> stack = new ArrayList<PackageDirectory>();

  /**
   * Create directory visitor.
   * @param root entry point to visit must be a directory
   */
  public DirectoryVisitor(File root) {
    myRoot = root;
    if (!myRoot.isDirectory()) {
      throw new IllegalArgumentException("Directory expected: " + myRoot.getAbsolutePath());
    }
  }

  public void visitFiles() {
    stack.clear();
    stack.add(new PackageDirectory(myRoot, ""));
    while (!stack.isEmpty()) {
      final PackageDirectory top = stack.remove(stack.size() - 1);
      final File[] children = top.file.listFiles();
      if (children == null) continue;
      for (File child : children) {
        if (child.isDirectory()) {
          final String packageName = ("".equals(top.packageName) ? "" : top.packageName + ".") + child.getName();
          stack.add(new PackageDirectory(child, packageName));
        } else {
          visitFile(top.packageName, child);
        }
      }
    }
  }

  /**
   * Visits single file.
   * @param packageName dot separated path to file
   * @param file file to visit
   */
  protected void visitFile(String packageName, File file) {
  }

  private static class PackageDirectory {
    public final File file;
    public final String packageName;

    private PackageDirectory(File file, String packageName) {
      this.file = file;
      this.packageName = packageName;
    }
  }
}
