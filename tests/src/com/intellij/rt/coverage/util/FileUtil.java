/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package com.intellij.rt.coverage.util;

import org.jetbrains.annotations.Nullable;

import java.io.File;

public class FileUtil {
  public static void delete(@Nullable File file) {
    if (file == null) return;
    if (!file.exists()) return;

    if (file.isDirectory()) {
      final File[] files = file.listFiles();
      if (files != null) for (File f : files) {
        delete(f);
      }
    } else {
      file.delete();
    }
  }
}
