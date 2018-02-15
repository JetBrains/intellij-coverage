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
import com.intellij.rt.coverage.data.TestDiscoveryIOUtil;
import com.intellij.rt.coverage.util.CoverageIOUtil;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class SingleTrFileReader {
  private File file;
  private final TIntObjectHashMap<String> dict = new TIntObjectHashMap<String>();

  public SingleTrFileReader(File file) {
    this.file = file;
  }

  public final void read() throws IOException {
    int bufferSize = Integer.parseInt(System.getProperty(SingleTrFileDiscoveryProtocolDataListener.BUFFER_SIZE, "32768"));
    DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(file), bufferSize));
    // TODO Count read bytes and report in case of exceptions
    boolean start = true;
    while (true) {
      final int read = input.read();
      if (read == -1) {
        debug("stream ended before finish marker received");
        input.close();
        return;
      }
      byte msgType = (byte) read;
      switch (msgType) {
        case SingleTrFileDiscoveryProtocolDataListener.START_MARKER:
          byte version = input.readByte();
          debug("start marker, format version: " + version);
          break;
        case SingleTrFileDiscoveryProtocolDataListener.FINISH_MARKER:
          debug("finish marker");
          input.close();
          return;
        case SingleTrFileDiscoveryProtocolDataListener.TEST_FINISHED_MARKER:
          debug("test data received");
          readData(input);
          break;
        case SingleTrFileDiscoveryProtocolDataListener.NAMES_DICTIONARY_PART_MARKER:
          debug("partial dictionary received");
          readDictionary(input);
          break;
        case SingleTrFileDiscoveryProtocolDataListener.METADATA_MARKER:
          debug("metadata received");
          readMetadata(input);
          break;
        case SingleTrFileDiscoveryProtocolDataListener.HEADER_START:
          final byte[] jtc = new byte[3];
          if (!start) throw new IllegalStateException("File header is not expected here");
          if (input.read(jtc) != 3) throw new IOException("Failed to read header fully");
          if (!Arrays.equals(jtc, SingleTrFileDiscoveryProtocolDataListener.HEADER_TAIL))
            throw new IOException("File header mismatch: I" + new String(jtc, "ASCII"));
          debug("file header");
          break;
        default:
          throw new IllegalStateException(String.format("Unknown input: %2X", msgType));
      }
      start = false;
    }
  }

  private void readData(DataInputStream input) throws IOException {
    String testClassName = readString(input);
    String testMethodName = readString(input);
    testProcessingStarted(testClassName, testMethodName);
    int classCount = CoverageIOUtil.readINT(input);
    while (classCount-- > 0) {
      String className = readString(input);
      classProcessingStarted(className);
      int methodCount = CoverageIOUtil.readINT(input);
      while (methodCount-- > 0) {
        String methodName = readString(input);
        processMethodName(methodName);
      }
      classProcessingFinished(className);
    }
    testProcessingFinished(testClassName, testMethodName);
  }

  private String readString(DataInputStream input) throws IOException {
    return dict.get(CoverageIOUtil.readINT(input));
  }

  private void readDictionary(DataInput r) throws IOException {
    TestDiscoveryIOUtil.readDictionary(r, new TestDiscoveryIOUtil.DictionaryProcessor() {
      public void process(int id, String name) {
        processDictionaryRecord(id, name);
      }
    });
  }

  private void readMetadata(DataInput r) throws IOException {
    int count = CoverageIOUtil.readINT(r);
    if (count == 0) return;
    final Map<String, String> result = new LinkedHashMap<String, String>();
    while (count-- > 0) {
      String key = CoverageIOUtil.readUTFFast(r);
      String value = CoverageIOUtil.readUTFFast(r);
      result.put(key, value);
    }
    processMetadata(result);
  }

  private String getName(String testClassName, String testMethodName) {
    if (testClassName == null) return testMethodName;
    else if (testMethodName == null) return testClassName;
    else return testClassName + "." + testMethodName;
  }

  protected void testProcessingStarted(String testClassName, String testMethodName) {
    testProcessingStarted(getName(testClassName, testMethodName));
  }

  protected void testProcessingFinished(String testClassName, String testMethodName) {
    testProcessingFinished(getName(testClassName, testMethodName));
  }

  protected void testProcessingFinished(String testName) {

  }

  protected void classProcessingFinished(String className) {

  }

  protected void processMethodName(String methodName) {

  }

  protected void classProcessingStarted(String className) {

  }

  protected void testProcessingStarted(String testName) {

  }

  protected void processMetadata(Map<String, String> metadata) {
  }

  protected void debug(String s) {
  }

  protected void processDictionaryRecord(int id, String name) {
    dict.put(id, name);
  }

  public static abstract class Sequential extends SingleTrFileReader {
    private String currentClassName;
    private String currentTestName;

    public Sequential(File file) {
      super(file);
    }

    protected abstract void processData(String testName, String className, String methodName);

    @Override
    protected void testProcessingFinished(String testName) {
      currentTestName = null;
    }

    @Override
    protected void classProcessingFinished(String className) {
      currentClassName = null;
    }

    @Override
    protected void processMethodName(String methodName) {
      processData(currentTestName, currentClassName, methodName);
    }

    @Override
    protected void classProcessingStarted(String className) {
      currentClassName = className;
    }

    @Override
    protected void testProcessingStarted(String testName) {
      currentTestName = testName;
    }
  }
}
