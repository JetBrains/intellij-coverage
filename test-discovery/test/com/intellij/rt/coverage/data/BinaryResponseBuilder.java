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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

// Does not support numbers greater than 196
class BinaryResponseBuilder {
  private final ArrayList<Byte> myBytes;

  BinaryResponseBuilder() {
    myBytes = new ArrayList<Byte>();
  }

  public BinaryResponseBuilder withHeader() {
    myBytes.addAll(Arrays.asList((byte) 0x49, (byte) 0x4a, (byte) 0x54, (byte) 0x43));
    return this;
  }

  public BinaryResponseBuilder withStart(int version) {
    myBytes.add((byte) 0x1);
    myBytes.add((byte) version);
    return this;
  }

  public BinaryResponseBuilder withTestResultStart(int classId, int methodId, int count) {
    myBytes.add((byte) 0x3);
    myBytes.add((byte) classId);
    myBytes.add((byte) methodId);
    myBytes.add((byte) count);
    return this;
  }

  public BinaryResponseBuilder withTestResultClass(int classId, int count) {
    myBytes.add((byte) classId);
    myBytes.add((byte) count);
    return this;
  }

  public BinaryResponseBuilder withTestResultMethodBeforeV4(int methodId) {
    myBytes.add((byte) methodId);
    return this;
  }

  public BinaryResponseBuilder withTestResultMethod(int[] methodId) {
    myBytes.add((byte) methodId.length);
    for (int entry : methodId) {
      myBytes.add((byte) entry);
    }
    return this;
  }

  public BinaryResponseBuilder withNoneAffectedFiles() {
    myBytes.add((byte) 0);
    return this;
  }

  public BinaryResponseBuilder withIncrementalDictionaryStart(int count) {
    myBytes.add((byte) 0x2);
    myBytes.add((byte) count);
    return this;
  }

  public BinaryResponseBuilder withDictionaryElement(int id, int... string) {
    myBytes.add((byte) id);
    myBytes.add((byte) string.length);
    for (int b : string) {
      myBytes.add((byte) b);
    }
    return this;
  }

  public BinaryResponseBuilder withBytes(int... bytes) {
    for (int b : bytes) {
      myBytes.add((byte) b);
    }
    return this;
  }

  public byte[] build() {
    myBytes.add((byte) 0x00); // finish marker

    final byte[] result = new byte[myBytes.size()];
    final Iterator<Byte> iterator = myBytes.iterator();
    for (int i = 0; i < result.length; i++) {
      result[i] = iterator.next();
    }
    return result;
  }
}
