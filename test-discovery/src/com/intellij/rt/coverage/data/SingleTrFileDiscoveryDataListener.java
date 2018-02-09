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
import org.jetbrains.coverage.gnu.trove.TIntIntIterator;
import org.jetbrains.coverage.gnu.trove.TObjectIntHashMap;
import org.jetbrains.coverage.gnu.trove.TObjectIntProcedure;

import java.io.*;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class SingleTrFileDiscoveryDataListener implements TestDiscoveryDataListener {
  @SuppressWarnings("WeakerAccess")
  public static final String TRACE_FILE = "org.jetbrains.instrumentation.trace.file";
  @SuppressWarnings("WeakerAccess")
  public static final String BUFFER_SIZE = "org.jetbrains.instrumentation.trace.file.buffer.size";
  @SuppressWarnings("WeakerAccess")
  public static final String FILE_VERSION = "org.jetbrains.instrumentation.trace.file.version";

  private static final int VERSION = 0x1;

  static final int START_MARKER = 0x1;
  static final int TEST_FINISHED_MARKER = 0x2;
  static final int NAMES_DICTIONARY_MARKER = 0x3;
  static final int NAMES_DICTIONARY_PART_MARKER = 0x4;

  private final LongDataOutputStream stream;
  private final NameEnumerator nameEnumerator;
  private final byte version;

  public SingleTrFileDiscoveryDataListener() throws Exception {
    final File myTraceFile = getCanonicalFile(new File(System.getProperty(TRACE_FILE, "td.tr")));
    int bufferSize = Integer.parseInt(System.getProperty(BUFFER_SIZE, "32768"));
    myTraceFile.getParentFile().mkdirs();
    version = Byte.parseByte(System.getProperty(FILE_VERSION, String.valueOf(VERSION)));
    stream = new LongDataOutputStream(new BufferedOutputStream(new FileOutputStream(myTraceFile), bufferSize));
    nameEnumerator = (version == 2) ? new NameEnumerator.Incremental() : new NameEnumerator();
    start();
  }

  // For tests
  SingleTrFileDiscoveryDataListener(LongDataOutputStream stream) throws Exception {
    this(stream, (byte) VERSION);
  }

  // For tests
  SingleTrFileDiscoveryDataListener(LongDataOutputStream stream, byte version) throws Exception {
    this.version = version;
    this.stream = stream;
    nameEnumerator = new NameEnumerator.Incremental();
    start();
  }

  private void start() throws IOException {
    this.stream.writeByte(START_MARKER);
    this.stream.writeByte(this.version);
  }

  public void testFinished(String testName, Map<Integer, boolean[]> classToVisitedMethods, Map<Integer, int[]> classToMethodNames) throws Exception {
    final int testNameId = nameEnumerator.enumerate(testName);
    writeDictionaryIncrementIfSupported();
    stream.writeByte(TEST_FINISHED_MARKER);
    CoverageIOUtil.writeINT(stream, testNameId);
    writeVisitedMethod(classToVisitedMethods, classToMethodNames, stream);
  }

  private boolean writeDictionaryIncrementIfSupported() throws IOException {
    if (version != 2) return false;
    final List<NameEnumerator.Incremental.NameAndId> increment = ((NameEnumerator.Incremental) nameEnumerator).getAndClearDataIncrement();
    if (increment.isEmpty()) return true;
    stream.writeByte(NAMES_DICTIONARY_PART_MARKER);
    CoverageIOUtil.writeINT(stream, increment.size());
    for (NameEnumerator.Incremental.NameAndId nameAndId : increment) {
      CoverageIOUtil.writeINT(stream, nameAndId.getId());
      CoverageIOUtil.writeUTF(stream, nameAndId.getName());
    }
    return true;
  }

  public void testsFinished() throws IOException {
    try {
      if (!writeDictionaryIncrementIfSupported()) {
        writeFullDictionary();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      stream.close();
    }
  }

  private void writeFullDictionary() throws IOException {
    stream.writeByte(NAMES_DICTIONARY_MARKER);
    long dictStartOffset = stream.total();
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
    stream.writeLong(dictStartOffset);
  }

  public NameEnumerator getIncrementalNameEnumerator() {
    return nameEnumerator;
  }

  private void writeVisitedMethod(Map<Integer, boolean[]> classToVisitedMethods,
                                  Map<Integer, int[]> classToMethodNames,
                                  DataOutput os) throws IOException {
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
    final TIntIntIterator iterator = classToUsedMethods.iterator();
    while (iterator.hasNext()) {
      iterator.advance();
      final int className = iterator.key();
      int usedMethodsCount = iterator.value();

      CoverageIOUtil.writeINT(os, className);
      CoverageIOUtil.writeINT(os, usedMethodsCount);

      final int[] methodNames = classToMethodNames.get(className);
      final boolean[] used = classToVisitedMethods.get(className);

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
