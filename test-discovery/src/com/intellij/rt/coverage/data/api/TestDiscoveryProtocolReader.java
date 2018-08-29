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

public interface TestDiscoveryProtocolReader {

  void testDiscoveryDataProcessingStarted(int version);

  void testDiscoveryDataProcessingFinished();

  MetadataReader createMetadataReader();

  ClassMetadataReader createClassMetadataReader();

  NameEnumeratorReader createNameEnumeratorReader();

  TestDataReader createTestDataReader(int classId, int methodId);

  void debug(String message);

  void error(String message);

  void error(Exception error);

  interface MetadataReader {
    void processMetadataEntry(String key, String value);
  }

  interface ClassMetadataReader {
    void classStarted(int classId);

    void file(int fileId);

    void method(int methodId, byte[] hash);

    void classFinished(int classId);

    void finished();
  }

  interface NameEnumeratorReader {
    void enumerate(String name, int id);
  }

  interface TestDataReader {
    void classProcessingStarted(int classId);

    void processUsedMethod(int methodId);

    void classProcessingFinished(int classId);

    void testDataProcessed();

    void processAffectedFile(int[] chunks);
  }
}
