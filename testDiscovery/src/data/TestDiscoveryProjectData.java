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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TestDiscoveryProjectData {
  public static final String PROJECT_DATA_OWNER = "com/intellij/rt/coverage/data/TestDiscoveryProjectData";

  public static final String TRACE_DIR = "org.jetbrains.instrumentation.trace.dir";
  protected static TestDiscoveryProjectData ourProjectData = new TestDiscoveryProjectData();

  private String myTraceDir = System.getProperty(TRACE_DIR, "");

  public void setTraceDir(String traceDir) {
    myTraceDir = traceDir;
  }

  public static TestDiscoveryProjectData getProjectData() {
    return ourProjectData;
  }

  private final ConcurrentMap<String, boolean[]> myClassToVisitedMethods = new ConcurrentHashMap<String, boolean[]>();
  private final ConcurrentMap<String, String[]> myClassToMethodNames = new ConcurrentHashMap<String, String[]>();

  // called from instrumented code during class's static init
  public static boolean[] trace(String className, boolean[] methodFlags, String[] methodNames) {
    return ourProjectData.traceLines(className, methodFlags, methodNames);
  }

  private synchronized boolean[] traceLines(String className, boolean[] methodFlags, String[] methodNames) {
    //System.out.println("Registering " + className);
    //assert methodFlags.length == methodNames.length;
    final boolean[] previousMethodFlags = myClassToVisitedMethods.putIfAbsent(className, methodFlags);

    if (previousMethodFlags != null) {
      //  assert previousMethodFlags.length == methodFlags.length;
      final String[] previousMethodNames = myClassToMethodNames.get(className);
      //assert previousMethodNames != null && previousMethodNames.length == methodNames.length;
    } else {
      myClassToMethodNames.put(className, methodNames);
    }
    return previousMethodFlags != null ? previousMethodFlags : methodFlags;
  }

  public synchronized void testDiscoveryEnded(final String name) {
    new File(myTraceDir).mkdirs();
    final File traceFile = new File(myTraceDir, name + ".tr");
    try {
      if (!traceFile.exists()) {
        traceFile.createNewFile();
      }
      DataOutputStream os = null;
      try {
        os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(traceFile), 64 * 1024));

        //saveOldTrace(os);

        Map<String, Integer> classToUsedMethods = new HashMap<String, Integer>();
        for (Map.Entry<String, boolean[]> o : myClassToVisitedMethods.entrySet()) {
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
        for (Map.Entry<String, boolean[]> o : myClassToVisitedMethods.entrySet()) {
          final boolean[] used = o.getValue();
          final String className = o.getKey();

          Integer usedMethodsCount = classToUsedMethods.get(className);
          if (usedMethodsCount == null) continue;

          CoverageIOUtil.writeUTF(os, className);
          CoverageIOUtil.writeINT(os, usedMethodsCount);

          String[] methodNames = myClassToMethodNames.get(className);
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
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public synchronized void testDiscoveryStarted(final String name) {
    for (Object e : myClassToVisitedMethods.entrySet()) {
      boolean[] used = (boolean[]) ((Map.Entry) e).getValue();
      for (int i = 0, len = used.length; i < len; ++i) {
        if (used[i]) used[i] = false;
      }
    }
  }

}
