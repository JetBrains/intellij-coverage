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

package com.intellij.rt.coverage.data.api;

import com.intellij.rt.coverage.data.SingleTrFileDiscoveryProtocolDataListener;
import com.intellij.rt.coverage.data.TestDiscoveryProtocolDataListener;
import com.intellij.rt.coverage.util.CoverageIOUtil;

import java.io.*;
import java.util.Arrays;

@SuppressWarnings({"unused", "WeakerAccess"})
public class TestDiscoveryProtocolUtil {
  public static void readFile(File file,
                              TestDiscoveryProtocolReader reader) throws IOException {
    int bufferSize = Integer.parseInt(System.getProperty(SingleTrFileDiscoveryProtocolDataListener.BUFFER_SIZE, "32768"));
    readSequentially(new BufferedInputStream(new FileInputStream(file), bufferSize), reader);
  }

  public static void readSequentially(InputStream testDiscoveryDataStream,
                                      TestDiscoveryProtocolReader reader) throws IOException {
    DataInputStream input = new DataInputStream(testDiscoveryDataStream);

    boolean start = true;
    Integer version = null;
    while (true) {
      final int read = input.read();
      if (read == -1) {
        reader.debug("stream ended before finish marker received");
        input.close();
        return;
      }
      byte msgType = (byte) read;
      switch (msgType) {
        case TestDiscoveryProtocolDataListener.START_MARKER:
          byte v = input.readByte();
          version = (int) v;
          reader.testDiscoveryDataProcessingStarted(version);
          reader.debug("start marker, format version: " + version);
          break;
        case TestDiscoveryProtocolDataListener.FINISH_MARKER:
          reader.testDiscoveryDataProcessingFinished();
          reader.debug("finish marker");
          input.close();
          return;
        case TestDiscoveryProtocolDataListener.TEST_FINISHED_MARKER:
          reader.debug("test data received");
          assert version != null;
          readTestData(input, reader, version);
          break;
        case TestDiscoveryProtocolDataListener.NAMES_DICTIONARY_PART_MARKER:
          reader.debug("partial dictionary received");
          readDictionary(input, reader);
          break;
        case TestDiscoveryProtocolDataListener.METADATA_MARKER:
          reader.debug("metadata received");
          readMetadata(input, reader);
          break;
        case TestDiscoveryProtocolDataListener.CLASS_METADATA_MARKER:
          reader.debug("class metadata received");
          readClassMetadata(input, reader);
          break;
        case SingleTrFileDiscoveryProtocolDataListener.HEADER_START:
          final byte[] jtc = new byte[3];
          if (!start) throw new IllegalStateException("File header is not expected here");
          if (input.read(jtc) != 3) throw new IOException("Failed to read header fully");
          if (!Arrays.equals(jtc, SingleTrFileDiscoveryProtocolDataListener.HEADER_TAIL))
            reader.error("File header mismatch: I" + new String(jtc, "ASCII"));
          reader.debug("file header");
          break;
        default:
          reader.error(String.format("Unknown input: %2X", msgType));
      }
      start = false;
    }
  }

  private static void readMetadata(DataInputStream input, TestDiscoveryProtocolReader reader) throws IOException {
    TestDiscoveryProtocolReader.MetadataReader metadataReader = reader.createMetadataReader();
    int count = CoverageIOUtil.readINT(input);
    if (count == 0) return;
    while (count-- > 0) {
      String key = CoverageIOUtil.readUTFFast(input);
      String value = CoverageIOUtil.readUTFFast(input);
      metadataReader.processMetadataEntry(key, value);
    }
  }

  private static void readClassMetadata(DataInputStream input, TestDiscoveryProtocolReader reader) throws IOException {
    TestDiscoveryProtocolReader.ClassMetadataReader metadataReader = reader.createClassMetadataReader();
    int classesCount = CoverageIOUtil.readINT(input);
    if (classesCount == 0) return;
    while (classesCount-- > 0) {
      final int classId = CoverageIOUtil.readINT(input);
      if (metadataReader != null) metadataReader.classStarted(classId);
      int filesCount = CoverageIOUtil.readINT(input);
      while (filesCount-- > 0) {
        final int fileId = CoverageIOUtil.readINT(input);
        if (metadataReader != null) metadataReader.file(fileId);
      }
      int methodsCount = CoverageIOUtil.readINT(input);
      while (methodsCount-- > 0) {
        final int methodId = CoverageIOUtil.readINT(input);
        final int hashLength = CoverageIOUtil.readINT(input);
        final byte[] hash = new byte[hashLength];
        final int read = input.read(hash);
        assert read == hashLength;
        if (metadataReader != null) metadataReader.method(methodId, hash);
      }
      if (metadataReader != null) metadataReader.classFinished(classId);
    }
    if (metadataReader != null) metadataReader.finished();
  }

  private static void readDictionary(DataInputStream input, TestDiscoveryProtocolReader reader) throws IOException {
    TestDiscoveryProtocolReader.NameEnumeratorReader nameEnumeratorReader = reader.createNameEnumeratorReader();
    int count = CoverageIOUtil.readINT(input);
    while (count-- > 0) {
      int id = CoverageIOUtil.readINT(input);
      String name = CoverageIOUtil.readUTFFast(input);
      nameEnumeratorReader.enumerate(name, id);
    }
  }

  private static void readTestData(DataInputStream input, TestDiscoveryProtocolReader reader, int protocolVersion) throws IOException {
    // read test name
    int testClassName = CoverageIOUtil.readINT(input);
    int testMethodName = CoverageIOUtil.readINT(input);
    TestDiscoveryProtocolReader.TestDataReader testDataReader = reader.createTestDataReader(testClassName, testMethodName);

    // read used methods
    int classCount = CoverageIOUtil.readINT(input);
    while (classCount-- > 0) {
      int classId = CoverageIOUtil.readINT(input);
      int methodCount = CoverageIOUtil.readINT(input);
      testDataReader.classProcessingStarted(classId);
      while (methodCount-- > 0) {
        int methodId = CoverageIOUtil.readINT(input);
        testDataReader.processUsedMethod(methodId);
      }
      testDataReader.classProcessingFinished(classId);
    }

    if (protocolVersion >= 3) {
      // read affected resource files
      int filesCount = CoverageIOUtil.readINT(input);
      while (filesCount-- > 0) {
        readFile(input, testDataReader);
      }
    }

    testDataReader.testDataProcessed();
  }

  private static void readFile(DataInputStream input, TestDiscoveryProtocolReader.TestDataReader testDataReader) throws IOException {
    int count = CoverageIOUtil.readINT(input);
    int[] chunks = new int[count];
    int len = count;
    while (count > 0) {
      int i = CoverageIOUtil.readINT(input);
      chunks[len - count] = i;
      count--;
    }
    testDataReader.processAffectedFile(chunks);
  }
}
