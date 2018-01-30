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

package com.intellij.rt.coverage.data;

import com.intellij.rt.coverage.util.CoverageIOUtil;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class TrFileDiscoveryDataListener implements TestDiscoveryDataListener {
  public static final String TRACE_DIR = "org.jetbrains.instrumentation.trace.dir";

  private final String myTraceDir;

  public TrFileDiscoveryDataListener() {
    myTraceDir = System.getProperty(TRACE_DIR);
    if (myTraceDir == null) {
      throw new NullPointerException("\'" + TRACE_DIR + "\' property should be specified to store test discovery .tr-files");
    }
  }

  public void testFinished(String testName, Map<String, boolean[]> classToVisitedMethods, Map<String, String[]> classToMethodNames) throws IOException {
    new File(myTraceDir).mkdirs();
    final File traceFile = new File(myTraceDir, testName + ".tr");
    if (!traceFile.exists()) {
      traceFile.createNewFile();
    }
    BufferedOutputStream fileStream = new BufferedOutputStream(new FileOutputStream(traceFile), 64 * 1024);
    writeVisitedMethods(classToVisitedMethods, classToMethodNames, fileStream);
  }

  public void testsFinished() {

  }

  private static void writeVisitedMethods(Map<String, boolean[]> classToVisitedMethods,
                                          Map<String, String[]> classToMethodNames,
                                          OutputStream stream) throws IOException {
    DataOutputStream os = null;
    try {
      os = new DataOutputStream(stream);

      //saveOldTrace(os);

      Map<String, Integer> classToUsedMethods = new HashMap<String, Integer>();
      for (Map.Entry<String, boolean[]> o : classToVisitedMethods.entrySet()) {
        boolean[] used = o.getValue();
        int usedMethodsCount = 0;

        for (boolean anUsed : used) {
          if (anUsed) ++usedMethodsCount;
        }

        if (usedMethodsCount > 0) {
          classToUsedMethods.put(o.getKey(), usedMethodsCount);
        }
      }

      CoverageIOUtil.writeINT(os, classToUsedMethods.size());
      for (Map.Entry<String, boolean[]> o : classToVisitedMethods.entrySet()) {
        final boolean[] used = o.getValue();
        final String className = o.getKey();

        Integer usedMethodsCount = classToUsedMethods.get(className);
        if (usedMethodsCount == null) continue;

        CoverageIOUtil.writeUTF(os, className);
        CoverageIOUtil.writeINT(os, usedMethodsCount);

        String[] methodNames = classToMethodNames.get(className);
        for (int i = 0, len = used.length; i < len; ++i) {
          // we check usedMethodCount here since used can still be updated by other threads
          if (used[i] && usedMethodsCount-- > 0) {
            CoverageIOUtil.writeUTF(os, methodNames[i]);
          }
        }
      }
    } finally {
      if (os != null) {
        os.close();
      }
    }
  }
}
