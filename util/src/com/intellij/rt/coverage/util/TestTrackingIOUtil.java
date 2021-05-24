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

package com.intellij.rt.coverage.util;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class TestTrackingIOUtil {
  public static void saveTestResults(File tracesDirectory, String name, Map<Object, boolean[]> trace) throws IOException {
    final File traceFile = new File(tracesDirectory, name + ".tr");
    if (!traceFile.exists()) {
      traceFile.createNewFile();
    }
    DataOutputStream os = null;
    try {
      os = new DataOutputStream(new FileOutputStream(traceFile));
      os.writeInt(trace.size());
      for (Map.Entry<Object, boolean[]> entry : trace.entrySet()) {
        os.writeUTF(entry.getKey().toString());
        final boolean[] lines = entry.getValue();
        int numberOfTraces = 0;
        for (boolean line : lines) {
          if (line) numberOfTraces++;
        }
        os.writeInt(numberOfTraces);
        for (int idx = 0; idx < lines.length; idx++) {
          final boolean incl = lines[idx];
          if (incl) {
            os.writeInt(idx);
          }
        }
      }
    } finally {
      if (os != null) {
        os.close();
      }
    }
  }

  public static Map<String, Map<String, int[]>> loadTestTrackingData(File tracesDirectory) throws IOException {
    final File[] traces = tracesDirectory.listFiles();
    final Map<String, Map<String, int[]>> result = new HashMap<String, Map<String, int[]>>();
    if (traces == null) return result;
    for (File traceFile : traces) {
      final String fileName = traceFile.getName();
      final String name = fileName.substring(0, fileName.length() - ".tr".length());
      final Map<String, int[]> classes = new HashMap<String, int[]>();
      DataInputStream is = null;
      try {
        is = new DataInputStream(new FileInputStream(traceFile));
        int size = is.readInt();
        for (int i = 0; i < size; i++) {
          final String className = is.readUTF();
          final int lines = is.readInt();
          final int[] coveredLines = new int[lines];
          for (int j = 0; j < lines; j++) {
            coveredLines[j] = is.readInt();
          }
          classes.put(className, coveredLines);
        }
      } finally {
        if (is != null) {
          is.close();
        }
      }

      result.put(name, classes);
    }
    return result;
  }
}
