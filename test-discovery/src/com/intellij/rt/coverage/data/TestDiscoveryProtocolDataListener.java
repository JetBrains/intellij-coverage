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

import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public abstract class TestDiscoveryProtocolDataListener implements TestDiscoveryDataListener {

  public static final int START_MARKER = 0x01;
  public static final int FINISH_MARKER = 0x00;

  public static final int NAMES_DICTIONARY_PART_MARKER = 0x02;
  public static final int TEST_FINISHED_MARKER = 0x03;

  public static final int METADATA_MARKER = 0x05;
  public static final int CLASS_METADATA_MARKER = 0x06;

  protected final byte myVersion;

  public TestDiscoveryProtocolDataListener(byte version) {
    this.myVersion = version;
  }

  @Override
  public byte getVersion() {
    return myVersion;
  }

  protected void writeTestFinished(DataOutput output, String className, String methodName,
                                   Map<Integer, boolean[]> classToVisitedMethods,
                                   Map<Integer, int[][]> classToMethodIds,
                                   List<int[]> openedFiles) throws IOException {
    NameEnumerator nameEnumerator = getNameEnumerator();
    final int testClassNameId = nameEnumerator.enumerate(className);
    final int testMethodNameId = nameEnumerator.enumerate(methodName);

    // Enumerator may send className and methodName if it's first test in class or this test caused classloading
    // Otherwise className and methodName was already sent with one of previous calls
    // Also send enumerated file path chunks
    writeDictionaryIncrementIfNeeded(output);

    output.writeByte(TEST_FINISHED_MARKER);
    CoverageIOUtil.writeINT(output, testClassNameId);
    CoverageIOUtil.writeINT(output, testMethodNameId);
    writeVisitedMethod(classToVisitedMethods, classToMethodIds, output);
    writeAffectedFiles(output, openedFiles);
  }

  private void writeAffectedFiles(DataOutput output, List<int[]> files) throws IOException {
    if (myVersion < 3) return;
    CoverageIOUtil.writeINT(output, files.size());
    for (int[] file : files) {
      CoverageIOUtil.writeINT(output, file.length);
      for (int i : file) {
        CoverageIOUtil.writeINT(output, i);
      }
    }
  }

  protected void start(DataOutput output) throws IOException {
    output.writeByte(START_MARKER);
    output.writeByte(myVersion);
  }

  protected void finish(DataOutput output) throws IOException {
    output.writeByte(FINISH_MARKER);
    // TODO: Write CRC or at least total length?
  }

  public abstract NameEnumerator.Incremental getNameEnumerator();

  protected void writeDictionaryIncrementIfNeeded(DataOutput output) throws IOException {
    final List<NameEnumerator.Incremental.NameAndId> increment = getNameEnumerator().getAndClearDataIncrement();
    if (increment.isEmpty()) return;
    output.writeByte(NAMES_DICTIONARY_PART_MARKER);
    writeEnumeratorIncrement(output, increment);
  }

  protected void writeEnumeratorIncrement(DataOutput output, List<NameEnumerator.Incremental.NameAndId> increment) throws IOException {
    CoverageIOUtil.writeINT(output, increment.size());
    for (NameEnumerator.Incremental.NameAndId nameAndId : increment) {
      CoverageIOUtil.writeINT(output, nameAndId.getId());
      CoverageIOUtil.writeUTF(output, nameAndId.getName());
    }
  }

  protected void writeVisitedMethod(Map<Integer, boolean[]> classToVisitedMethods,
                                    Map<Integer, int[][]> classToMethodIds,
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

      final int[][] methodIds = classToMethodIds.get(className);
      final boolean[] used = classToVisitedMethods.get(className);

      for (int i = 0, len = used.length; i < len; ++i) {
        // we check usedMethodCount here since used can still be updated by other threads
        if (used[i] && usedMethodsCount-- > 0) {
          int[] methodId = methodIds[i];
          if (myVersion >= 4) {
            CoverageIOUtil.writeINT(os, methodId.length);
            for (int entry : methodId) {
              CoverageIOUtil.writeINT(os, entry);
            }
          } else {
            CoverageIOUtil.writeINT(os, methodId[0]);
          }
        }
      }
    }
  }

  /**
   * Writes file metadata map as list of key-value pairs.
   * Format:
   * <ul>
   * <li>Marker - byte</li>
   * <li>N - number</li>
   * <li>Key1 - string</li>
   * <li>Value1 - string</li>
   * <li>...</li>
   * <li>KeyN - string</li>
   * <li>ValueN - string</li>
   * </ul>
   * <p>
   * Note that enumerator is not used since metadata is usually small
   */
  protected void writeMetadata(DataOutput os, Map<String, String> metadata) throws IOException {
    if (metadata == null || metadata.isEmpty()) return;
    final LinkedHashMap<String, String> map = new LinkedHashMap<String, String>(metadata);
    os.writeByte(METADATA_MARKER);
    CoverageIOUtil.writeINT(os, map.size());
    for (Map.Entry<String, String> entry : map.entrySet()) {
      CoverageIOUtil.writeUTF(os, entry.getKey());
      CoverageIOUtil.writeUTF(os, entry.getValue());
    }
  }

  /**
   * Writes class metadata
   * Format:
   * <ul>
   * <li>Marker - byte</li>
   * <li>Class.Count (N) - number</li>
   * <li>Class[1] - number</li>
   * <li>Class[1].Files.Count (M) - number</li>
   * <li>Class[1].Files[1] - number</li>
   * <li>...</li>
   * <li>Class[1].Files[M] - number</li>
   * <li>Class[1].Methods.Count (K) - number</li>
   * <li>Class[1].Method[1].Id - number</li>
   * <li>Class[1].Method[1].Hash.Length - number</li>
   * <li>Class[1].Method[1].Hash - byte-array</li>
   * <li>...</li>
   * <li>Class[1].Method[K].Id - number</li>
   * <li>Class[1].Method[K].Hash.Length - number</li>
   * <li>Class[1].Method[K].Hash - byte-array</li>
   * <li>Class[2] - number</li>
   * <li>...</li>
   * </ul>
   * <p>
   * Note that enumerator is used and enumerator diff may be sent first
   *
   * @since Version 2
   */
  protected void writeClassMetadata(DataOutput os, List<ClassMetadata> metadata) throws IOException {
    if (metadata == null || metadata.isEmpty()) return;
    if (myVersion < 2) return;

    final NameEnumerator.Incremental enumerator = getNameEnumerator();

    // Prepare data for enumerator
    for (ClassMetadata data : metadata) {
      enumerator.enumerate(data.getFqn());
      if (data.getFiles() != null) for (String file : data.getFiles()) {
        enumerator.enumerate(file);
      }
      if (data.getMethods() != null) for (String method : data.getMethods().keySet()) {
        enumerator.enumerate(method);
      }
    }

    writeDictionaryIncrementIfNeeded(os);

    final ArrayList<ClassMetadata> list = new ArrayList<ClassMetadata>(metadata);

    os.writeByte(CLASS_METADATA_MARKER);
    CoverageIOUtil.writeINT(os, list.size());
    for (ClassMetadata data : list) {
      // Class ID
      CoverageIOUtil.writeINT(os, enumerator.enumerate(data.getFqn()));
      // Files array
      final List<String> files = data.getFiles();
      if (files == null) {
        CoverageIOUtil.writeINT(os, 0);
      } else {
        CoverageIOUtil.writeINT(os, files.size());
        for (String file : files) {
          CoverageIOUtil.writeINT(os, enumerator.enumerate(file));
        }
      }
      // Methods array
      final Map<String, byte[]> methods = data.getMethods();
      if (methods == null) {
        CoverageIOUtil.writeINT(os, 0);
      } else {
        CoverageIOUtil.writeINT(os, methods.size());
        for (Map.Entry<String, byte[]> method : methods.entrySet()) {
          CoverageIOUtil.writeINT(os, enumerator.enumerate(method.getKey()));
          CoverageIOUtil.writeINT(os, method.getValue().length);
          os.write(method.getValue());
        }
      }
    }
  }
}
