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
import org.jetbrains.coverage.gnu.trove.TIntIntHashMap;
import org.jetbrains.coverage.gnu.trove.TObjectIntHashMap;
import org.jetbrains.coverage.gnu.trove.TObjectIntProcedure;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("unused")
public class SingleTrFileDiscoveryDataListener implements TestDiscoveryDataListener {
  @SuppressWarnings("WeakerAccess")
  public static final String TRACE_FILE = "org.jetbrains.instrumentation.trace.file";
  @SuppressWarnings("WeakerAccess")
  public static final String BUFFER_SIZE = "org.jetbrains.instrumentation.trace.file.buffer.size";

  private static final int VERSION = 0x1;

  static final int START_MARKER = 0x1;
  static final int TEST_FINISHED_MARKER = 0x2;
  static final int NAMES_DICTIONARY_MARKER = 0x3;

  private final DataOutputStream stream;
  private final NameEnumerator nameEnumerator = new NameEnumerator();

  public SingleTrFileDiscoveryDataListener() throws Exception {
    final File myTraceFile = getCanonicalFile(new File(System.getProperty(TRACE_FILE, "td.tr")));
    int bufferSize = Integer.parseInt(System.getProperty(BUFFER_SIZE, "32768"));
    myTraceFile.getParentFile().mkdirs();
    stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(myTraceFile), bufferSize));
    stream.writeByte(START_MARKER);
    stream.writeByte(VERSION);
  }

  public SingleTrFileDiscoveryDataListener(DataOutputStream stream) throws Exception {
    this.stream = stream;
    this.stream.writeByte(START_MARKER);
    this.stream.writeByte(VERSION);
  }

  public void testFinished(String testName, ConcurrentMap<Integer, boolean[]> classToVisitedMethods, ConcurrentMap<Integer, int[]> classToMethodNames) throws Exception {
    stream.writeByte(TEST_FINISHED_MARKER);
    CoverageIOUtil.writeINT(stream, nameEnumerator.enumerate(testName));
    writeVisitedMethod(classToVisitedMethods, classToMethodNames, stream);
  }

  public void testsFinished() throws IOException {
    try {
      stream.writeByte(NAMES_DICTIONARY_MARKER);
      int dictStartOffset = stream.size();
      TObjectIntHashMap<String> namesMap = nameEnumerator.getNamesMap();
      CoverageIOUtil.writeINT(stream, namesMap.size());
      namesMap.forEachEntry(new TObjectIntProcedure<String>() {
        public boolean execute(String s, int i) {
          try {
            CoverageIOUtil.writeINT(stream, i);
            CoverageIOUtil.writeUTF(stream, s);
          } catch (IOException e) {
            e.printStackTrace();
          }
          return true;
        }
      });
      stream.writeInt(dictStartOffset);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      stream.close();
    }
  }

  public NameEnumerator getIncrementalNameEnumerator() {
    return nameEnumerator;
  }

  private void writeVisitedMethod(Map<Integer, boolean[]> classToVisitedMethods,
                                  Map<Integer, int[]> classToMethodNames,
                                  DataOutputStream os) throws IOException {
    TIntIntHashMap classToUsedMethods = new TIntIntHashMap();
    for (Map.Entry<Integer, boolean[]> o : classToVisitedMethods.entrySet()) {
      boolean[] used = o.getValue();
      int usedMethodsCount = 0;

      for (boolean anUsed : used) {
        if (anUsed) ++usedMethodsCount;
      }

      if (usedMethodsCount > 0) {
        classToUsedMethods.put(o.getKey(), usedMethodsCount);
      }
    }

    final int size = classToUsedMethods.size();
    CoverageIOUtil.writeINT(os, size);
    if (size == 0) return;
    for (Map.Entry<Integer, boolean[]> o : classToVisitedMethods.entrySet()) {
      final boolean[] used = o.getValue();
      final int className = o.getKey();

      if (!classToUsedMethods.containsKey(className)) continue;
      int usedMethodsCount = classToUsedMethods.get(className);

      CoverageIOUtil.writeINT(os, className);
      CoverageIOUtil.writeINT(os, usedMethodsCount);

      int[] methodNames = classToMethodNames.get(className);
      for (int i = 0, len = used.length; i < len; ++i) {
        // we check usedMethodCount here since used can still be updated by other threads
        if (used[i] && usedMethodsCount-- > 0) {
          CoverageIOUtil.writeINT(os, methodNames[i]);
        }
      }
    }
  }

  private static File getCanonicalFile(File file) {
    try {
      return file.getCanonicalFile();
    } catch (IOException e) {
      return file.getAbsoluteFile();
    }
  }
}
