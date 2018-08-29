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

public final class BinaryResponses {
  private BinaryResponses() {
  }

  public static byte[] empty(int version) {
    return new BinaryResponseBuilder()
        .withHeader().withStart(version)
        .build();
  }

  public static byte[] metadata(int version) {
    return new BinaryResponseBuilder()
        .withHeader().withStart(version)
        .withBytes(0x5, 0x1, 0x1, 0x41, 0x1, 0x42)
        .build();
  }

  public static byte[] noTestsOneName(int version) {
    return new BinaryResponseBuilder()
        .withHeader().withStart(version)
        .withIncrementalDictionaryStart(1)
        .withDictionaryElement(1, 0x41, 0x42, 0x43) // 1-ABC
        .build();
  }

  public static byte[] singleTestNoMethods(int version) {
    return new BinaryResponseBuilder()
        .withHeader().withStart(version)
        .withIncrementalDictionaryStart(1)
        .withDictionaryElement(1, 0x41, 0x42, 0x43) // 1-ABC
        .withTestResultStart(1, 1, 0) // Test ABC.ABC, 0 coverage
        .withNoneAffectedFiles()
        .build();
  }

  public static byte[] singleTestSingleMethod(int version) {
    return new BinaryResponseBuilder()
        .withHeader().withStart(version)
        .withIncrementalDictionaryStart(3)
        .withDictionaryElement(1, 0x41) // 1-A
        .withDictionaryElement(2, 0x42) // 2-B
        .withDictionaryElement(3, 0x43) // 3-C
        .withTestResultStart(1, 2, 1) // Test A.B, 1 class
        .withTestResultClass(2, 1) // Class B, 1 method
        .withTestResultMethod(3) // Method C
        .withNoneAffectedFiles()
        .build();
  }
}
