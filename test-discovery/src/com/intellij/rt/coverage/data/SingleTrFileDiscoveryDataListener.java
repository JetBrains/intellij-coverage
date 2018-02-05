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
import org.jetbrains.coverage.gnu.trove.TObjectIntHashMap;
import org.jetbrains.coverage.gnu.trove.TObjectIntProcedure;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

@SuppressWarnings("unused")
public class SingleTrFileDiscoveryDataListener extends TrFileDiscoveryDataListener {
  @SuppressWarnings("WeakerAccess")
  public static final String TRACE_FILE = "org.jetbrains.instrumentation.trace.file";
  @SuppressWarnings("WeakerAccess")
  public static final String BUFFER_SIZE = "org.jetbrains.instrumentation.trace.file.buffer.size";

  private static final int VERSION = 0x1;

  private static final int START_MARKER = 0x1;
  private static final int TEST_FINISHED_MARKER = 0x2;
  private static final int NAMES_DICTIONARY_MARKER = 0x3;

  private final DataOutputStream stream;
  private final IncrementalNameEnumerator nameEnumerator = new IncrementalNameEnumerator();

  public SingleTrFileDiscoveryDataListener() throws Exception {
    String myTraceFile = System.getProperty(TRACE_FILE, "td.tr");
    int bufferSize = Integer.parseInt(System.getProperty(BUFFER_SIZE, "32768"));
    stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(myTraceFile), bufferSize));
    stream.writeByte(START_MARKER);
    stream.writeByte(VERSION);
  }

  public void testFinished(String testName, Map<String, boolean[]> classToVisitedMethods, Map<String, String[]> classToMethodNames) throws IOException {
    stream.writeByte(TEST_FINISHED_MARKER);
    writeVisitedMethod(classToVisitedMethods, classToMethodNames, stream);
  }

  public void testsFinished() throws IOException {
    try {
      stream.writeByte(NAMES_DICTIONARY_MARKER);
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
    } catch (IOException e) {
      e.printStackTrace();
    }
    finally {
      stream.close();
    }
  }

  protected void writeString(DataOutputStream os, String className) throws IOException {
    CoverageIOUtil.writeINT(os, nameEnumerator.enumerate(className));
  }
}
