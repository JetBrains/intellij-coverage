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

package com.intellij.rt.coverage.offline;

import com.intellij.rt.coverage.util.CoverageIOUtil;
import com.intellij.rt.coverage.util.ErrorReporter;

import java.io.*;

/**
 * This report is used in case of offline instrumentation.
 * As no class file analysis is done in runtime, this report stores coverage results
 * in row format. It includes only class names and arrays with hits.
 * On load, this information could be applied to a ProjectData with already analysed classes.
 */
public class RawHitsReport {
  private static final int MAGIC = 284996684;

  public static void dump(DataOutput output, RawProjectData data) throws IOException {
    CoverageIOUtil.writeINT(output, MAGIC);

    // leave empty line as a space for format configuration
    CoverageIOUtil.writeUTF(output, "");

    for (RawClassData classData : data.getClasses()) {
      final int[] hits = classData.hits;
      if (hits == null || hits.length == 0) continue;
      CoverageIOUtil.writeUTF(output, classData.name);
      CoverageIOUtil.writeINT(output, hits.length);
      for (int hit : hits) {
        CoverageIOUtil.writeINT(output, hit);
      }
    }

    // file end marker
    CoverageIOUtil.writeUTF(output, "");
  }

  public static RawProjectData load(File file) throws IOException {
    final RawProjectData projectData = new RawProjectData();
    DataInputStream is = null;
    try {
      is = CoverageIOUtil.openReadFile(file);

      final int magic = CoverageIOUtil.readINT(is);
      if (magic != MAGIC) {
        throw new IOException("This file is not in raw hits report format");
      }

      // read config line
      CoverageIOUtil.readUTFFast(is);

      String className;
      while (!"".equals(className = CoverageIOUtil.readUTFFast(is))) {
        final int length = CoverageIOUtil.readINT(is);
        final int[] hits = projectData.getOrCreateClass(className, length).hits;
        for (int i = 0; i < length; i++) {
          hits[i] = CoverageIOUtil.readINT(is);
        }
      }
    } finally {
      CoverageIOUtil.close(is);
    }
    return projectData;
  }

  public static boolean isRawHitsFile(File file) throws IOException {
    DataInputStream is = null;
    try {
      is = CoverageIOUtil.openReadFile(file);
      final int magic = CoverageIOUtil.readINT(is);
      return magic == MAGIC;
    } finally {
      CoverageIOUtil.close(is);
    }
  }
}
