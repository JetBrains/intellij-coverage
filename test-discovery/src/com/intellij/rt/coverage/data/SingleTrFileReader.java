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
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;

import java.io.*;

public class SingleTrFileReader {
  private File file;
  private final TIntObjectHashMap<String> dict = new TIntObjectHashMap<String>();

  public SingleTrFileReader(File file) {
    this.file = file;
  }

  public final void read() throws IOException {
    int bufferSize = Integer.parseInt(System.getProperty(SingleTrFileDiscoveryDataListener.BUFFER_SIZE, "32768"));
    DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(file), bufferSize));
    while (true) {
      final int read = input.read();
      if (read == -1) {
        debug("stream ended while in zero state");
        input.close();
        return;
      }
      byte msgType = (byte) read;
      switch (msgType) {
        case SingleTrFileDiscoveryDataListener.START_MARKER:
          byte version = input.readByte();
          debug("version: " + version);
          if (version == 1) {
            readDictionary();
          }
          break;
        case SingleTrFileDiscoveryDataListener.NAMES_DICTIONARY_MARKER:
          debug("test data ended");
          input.close();
          return;
        case SingleTrFileDiscoveryDataListener.TEST_FINISHED_MARKER:
          debug("test data received");
          readData(input);
          break;
        case SingleTrFileDiscoveryDataListener.NAMES_DICTIONARY_PART_MARKER:
          debug("partial dictionary received");
          readDictionary(input);
          break;
      }
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

  private void readDictionary() throws IOException {
    int size = 8;
    RandomAccessFile r = null;
    try {
      r = new RandomAccessFile(file, "r");
      if (r.length() < size + 1) {
        throw new IOException("Dictionary not found: file is too small");
      }
      r.seek(r.length() - size);
      long dictOffset = r.readLong();
      if (dictOffset > r.length() || dictOffset < 0) {
        throw new IOException("Dictionary not found: offset specified in the end of file is outside of file range");
      }
      r.seek(dictOffset - 1);
      if (r.readByte() != SingleTrFileDiscoveryDataListener.NAMES_DICTIONARY_MARKER) {
        throw new IOException("Dictionary not found: offset specified in the end of file is incorrect");
      }
      readDictionary(r);
    } finally {
      if (r != null) {
        r.close();
      }
    }
  }

  private void readDictionary(DataInput r) throws IOException {
    TestDiscoveryIOUtil.readDictionary(r, new TestDiscoveryIOUtil.DictionaryProcessor() {
      public void process(int id, String name) {
        processDictionaryRecord(id, name);
      }
    });
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
