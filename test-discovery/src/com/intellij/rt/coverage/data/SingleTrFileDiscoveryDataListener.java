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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.intellij.rt.coverage.data.TrFileDiscoveryDataListener.writeVisitedMethod;

public class SingleTrFileDiscoveryDataListener implements TestDiscoveryDataListener  {
  @SuppressWarnings("WeakerAccess")
  public static final String TRACE_FILE = "org.jetbrains.instrumentation.trace.file";
  @SuppressWarnings("WeakerAccess")
  public static final String BUFFER_SIZE = "org.jetbrains.instrumentation.trace.file.buffer.size";

  private final BufferedOutputStream bw;
  private final IncrementalNameEnumerator nameEnumerator;

  public SingleTrFileDiscoveryDataListener() throws Exception {
    String myTraceFile = System.getProperty(TRACE_FILE, "td.tr");
    nameEnumerator = new IncrementalNameEnumerator();
    bw = new BufferedOutputStream(new FileOutputStream(myTraceFile), Integer.parseInt(System.getProperty(BUFFER_SIZE, "32768")));
  }

  public void testFinished(String testName, Map<String, boolean[]> classToVisitedMethods, Map<String, String[]> classToMethodNames) throws IOException {
    writeVisitedMethod(classToVisitedMethods, classToMethodNames, new DataOutputStream(bw) {
      @Override
      public void close() {
      }
    }, nameEnumerator);
  }

  public void testsFinished() {
    try {
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  protected static void writeVisitedMethod(Map<String, boolean[]> classToVisitedMethods,
                                           Map<String, String[]> classToMethodNames,
                                           DataOutputStream os,
                                           IncrementalNameEnumerator nameEnumerator) throws IOException {
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

      ByteBuffer enumerate = nameEnumerator.enumerate(className);

      os.writeInt();
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
  }

}
