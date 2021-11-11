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

import java.io.File;
import java.util.Collections;
import java.util.List;

public class ClassFileLocator extends FileLocator {
  public ClassFileLocator(List<File> roots) {
    super(roots);
  }

  @Override
  public List<File> locate(String fqName) {
    final int packageIndex = fqName.lastIndexOf('.');
    if (packageIndex < 0) return Collections.emptyList();
    final String packageName = fqName.substring(0, packageIndex);
    final String className = fqName.substring(packageIndex + 1);
    return locateFile(packageName, className + ".class");
  }
}
