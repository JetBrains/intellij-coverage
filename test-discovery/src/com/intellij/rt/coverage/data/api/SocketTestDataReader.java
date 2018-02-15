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

import com.intellij.rt.coverage.data.TestDiscoveryIOUtil;
import com.intellij.rt.coverage.util.CoverageIOUtil;

import java.io.DataInput;
import java.io.IOException;

public abstract class SocketTestDataReader {
  protected void processTestName(int testClassId, int testMethodId) {}

  protected void processUsedMethod(int classId, int methodId) {}

  protected void processEnumeratedName(int id, String name) {}

  public static void readDictionary(DataInput in, final SocketTestDataReader reader) throws IOException {
    // read enumerator increment
    TestDiscoveryIOUtil.readDictionary(in, new TestDiscoveryIOUtil.DictionaryProcessor() {
      public void process(int id, String name) {
        reader.processEnumeratedName(id, name);
      }
    });
  }

  public static void readTestData(DataInput in, final SocketTestDataReader reader) throws IOException {
    // read test name
    int testClassName = CoverageIOUtil.readINT(in);
    int testMethodName = CoverageIOUtil.readINT(in);
    reader.processTestName(testClassName, testMethodName);

    // read used methods
    int classCount = CoverageIOUtil.readINT(in);
    while (classCount-- > 0) {
      int classId = CoverageIOUtil.readINT(in);
      int methodCount = CoverageIOUtil.readINT(in);
      while (methodCount-- > 0) {
        int methodId = CoverageIOUtil.readINT(in);
        reader.processUsedMethod(classId, methodId);
      }
    }
  }
}
