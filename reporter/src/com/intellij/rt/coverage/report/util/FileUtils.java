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

import com.intellij.rt.coverage.util.CoverageIOUtil;

import java.io.*;

public class FileUtils {
  public static String readAll(File file) throws IOException {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(file));
      final StringBuilder result = new StringBuilder();
      String line = reader.readLine();
      while (line != null) {
        result.append(line).append('\n');
        line = reader.readLine();
      }
      return result.toString();
    } finally {
      CoverageIOUtil.close(reader);
    }
  }

  public static void write(File file, String data) throws IOException {
    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(file));
      writer.write(data);
    } finally {
      CoverageIOUtil.close(writer);
    }
  }
}
