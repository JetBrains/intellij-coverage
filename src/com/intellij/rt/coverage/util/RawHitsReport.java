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

package com.intellij.rt.coverage.util;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.ProjectData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * This report is used in case of offline instrumentation.
 * As no class file analysis is done in runtime, this report stores coverage results
 * in row format. It includes only class names and arrays with hits.
 * On load, this information could be applied to a ProjectData with already analysed classes.
 */
public class RawHitsReport {
  private static final int MAGIC = 284996684;

  public static void dump(File file, ProjectData data) {
    DataOutputStream os = null;
    try {
      os = CoverageIOUtil.openWriteFile(file);

      CoverageIOUtil.writeINT(os, MAGIC);

      // leave empty line as a space for format configuration
      CoverageIOUtil.writeUTF(os, "");

      for (ClassData classData : data.getClassesCollection()) {
        final int[] hits = classData.getHitsMask();
        if (hits == null || hits.length == 0) continue;
        CoverageIOUtil.writeUTF(os, classData.getName());
        CoverageIOUtil.writeINT(os, hits.length);
        for (int hit : hits) {
          CoverageIOUtil.writeINT(os, hit);
        }
      }

      // file end marker
      CoverageIOUtil.writeUTF(os, "");
    } catch (Throwable e) {
      ErrorReporter.reportError("Error during coverage report dump", e);
    } finally {
      CoverageIOUtil.close(os);
    }
  }

  public static void load(File file, ProjectData data) throws IOException {
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
        final ClassData classData = data.getClassData(className);
        if (classData == null) {
          ErrorReporter.logInfo("Tried to apply coverage for class " + className + " but there is no such class in ProjectData");
          continue;
        }
        final int length = CoverageIOUtil.readINT(is);
        final int[] hits = classData.getOrCreateHitsMask(length);
        for (int i = 0; i < length; i++) {
          hits[i] = CoverageIOUtil.readINT(is);
        }
        if (data.isSampling()) {
          classData.applyLinesMask();
        } else {
          classData.applyBranches();
        }
      }
    } finally {
      CoverageIOUtil.close(is);
    }
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

  public static void dumpOnExit(final File file, final ProjectData data) {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      public void run() {
        dump(file, data);
      }
    }));
  }
}
