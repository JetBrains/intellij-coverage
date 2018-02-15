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

import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;

public abstract class SimpleDecodingTestDiscoveryProtocolReader implements
    TestDiscoveryProtocolReader,
    TestDiscoveryProtocolReader.NameEnumeratorReader,
    TestDiscoveryProtocolReader.MetadataReader {
  private final TIntObjectHashMap<String> enumerator = new TIntObjectHashMap<String>();

  protected abstract void processData(String testClassName, String testMethodName, String className, String methodName);

  public void debug(String message) {

  }

  public void error(String message) {

  }

  public void error(Exception error) {

  }


  public void testDiscoveryDataProcessingStarted(int version) {

  }

  public void testDiscoveryDataProcessingFinished() {

  }

  public MetadataReader createMetadataReader() {
    return this;
  }

  public NameEnumeratorReader createNameEnumeratorReader() {
    return this;
  }


  public void enumerate(String name, int id) {
    enumerator.put(id, name);
  }

  public TestDataReader createTestDataReader(final int testClassId, final int testMethodId) {
    return new TestDataReader() {
      private String currentClassName;

      public void classProcessingStarted(int classId) {
        currentClassName = enumerator.get(classId);
      }

      public void processUsedMethod(int methodId) {
        processData(enumerator.get(testClassId), enumerator.get(testMethodId), currentClassName, enumerator.get(methodId));
      }

      public void classProcessingFinished(int classId) {

      }

      public void testDataProcessed() {

      }
    };
  }
}
