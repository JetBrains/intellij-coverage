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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SingleTrFileDiscoveryDataListenerTest {
  @Parameters
  public static Object[] versions() {
    return TraceFileVersions.VERSIONS;
  }

  @Parameter
  public int version;

  @Test
  public void testSingleTestNothingVisited() throws Exception {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final DataOutputStream dos = new DataOutputStream(baos);
    final SingleTrFileDiscoveryProtocolDataListener listener = new SingleTrFileDiscoveryProtocolDataListener(dos, version);
    final String name = "ABC";
    listener.getNameEnumerator().enumerate(name);
    final Map<Integer, boolean[]> classes = new HashMap<Integer, boolean[]>();
    final Map<Integer, int[]> methods = new HashMap<Integer, int[]>();
    classes.put(1, new boolean[]{false});
    methods.put(1, new int[]{1});
    listener.testFinished(name, name, classes, methods);
    listener.testsFinished();
    assertThat(baos.toByteArray()).isEqualTo(BinaryResponses.singleTestNoMethods(version));
  }

  @Test
  public void testV2NoCoverage() throws Exception {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final DataOutputStream dos = new DataOutputStream(baos);
    final SingleTrFileDiscoveryProtocolDataListener listener = new SingleTrFileDiscoveryProtocolDataListener(dos, version);
    listener.testsFinished();
    assertThat(baos.toByteArray()).isEqualTo(BinaryResponses.empty(version));
  }

  @Test
  public void testMetadata() throws Exception {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final DataOutputStream dos = new DataOutputStream(baos);
    final SingleTrFileDiscoveryProtocolDataListener listener = new SingleTrFileDiscoveryProtocolDataListener(dos, version);
    listener.addMetadata(Collections.singletonMap("A", "B"));
    listener.testsFinished();
    assertThat(baos.toByteArray()).isEqualTo(BinaryResponses.metadata(version));
  }

  @Test
  public void testClassMetadata() throws Exception {
    byte[] classMetadata = new BinaryResponseBuilder()
        .withHeader().withStart(2)
        .withIncrementalDictionaryStart(3)
        .withDictionaryElement(1, 0x41) // 1-A
        .withDictionaryElement(2, 0x41, 0x2e, 0x6a, 0x61, 0x76, 0x61) // 2-A.java
        .withDictionaryElement(3, 0x43) // 3-C
        .withBytes(0x6, 0x1, // 1 class
            0x1, // class name
            0x1, // 1 file
            0x2, // file name
            0x1, // 1 method
            0x3, // method name
            0x4, // method hash length
            0xCA, 0xFE, 0xBA, 0xBE // method hash
        ).build();
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final DataOutputStream dos = new DataOutputStream(baos);
    final SingleTrFileDiscoveryProtocolDataListener listener = new SingleTrFileDiscoveryProtocolDataListener(dos, 2);
    listener.addClassMetadata(singletonList(new ClassMetadata("A", singletonList("A.java"), Collections.singletonMap("C", new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}))));
    listener.testsFinished();
    assertThat(baos.toByteArray()).isEqualTo(classMetadata);
  }

  @Test
  public void testV2OnlyEnumerator() throws Exception {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final DataOutputStream dos = new DataOutputStream(baos);
    final SingleTrFileDiscoveryProtocolDataListener listener = new SingleTrFileDiscoveryProtocolDataListener(dos, version);
    listener.getNameEnumerator().enumerate("ABC");
    listener.testsFinished();
    assertThat(baos.toByteArray()).isEqualTo(BinaryResponses.noTestsOneName(version));
  }

  @Test
  public void testV2SingleTestOnlyOneClassVisited() throws Exception {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final DataOutputStream dos = new DataOutputStream(baos);
    final SingleTrFileDiscoveryProtocolDataListener listener = new SingleTrFileDiscoveryProtocolDataListener(dos, version);

    final String name1 = "A";
    final String name2 = "B";
    final String name3 = "C";
    listener.getNameEnumerator().enumerate(name1);
    listener.getNameEnumerator().enumerate(name2);
    listener.getNameEnumerator().enumerate(name3);

    final Map<Integer, boolean[]> classes = new HashMap<Integer, boolean[]>();
    final Map<Integer, int[]> methods = new HashMap<Integer, int[]>();
    classes.put(1, new boolean[]{false});
    classes.put(2, new boolean[]{true});
    methods.put(1, new int[]{1});
    methods.put(2, new int[]{3});
    listener.testFinished(name1, name2, classes, methods);
    listener.testsFinished();
    assertThat(baos.toByteArray()).isEqualTo(BinaryResponses.singleTestSingleMethod(version));
  }

  @Test
  public void testV2TwoTestsIncrementalDict() throws Exception {
    byte[] twoTestsIncrementalDict = new BinaryResponseBuilder()
        .withHeader().withStart(version)
        .withIncrementalDictionaryStart(2)
        .withDictionaryElement(1, 0x41) // 1-A
        .withDictionaryElement(2, 0x42) // 2-B
        .withTestResultStart(1, 2, 1) // Test A.B, 1 class
        .withTestResultClass(1, 1) // Class A, 1 method
        .withTestResultMethod(1) // Method A
        .withTestResultStart(2, 1, 1) // Test B.A, 1 class
        .withTestResultClass(1, 1) // Class A, 1 method
        .withTestResultMethod(1) // Method A
        .build();
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final DataOutputStream dos = new DataOutputStream(baos);
    final SingleTrFileDiscoveryProtocolDataListener listener = new SingleTrFileDiscoveryProtocolDataListener(dos, version);

    listener.getNameEnumerator().enumerate("A");

    final Map<Integer, boolean[]> classes = new HashMap<Integer, boolean[]>();
    final Map<Integer, int[]> methods = new HashMap<Integer, int[]>();
    classes.put(1, new boolean[]{true});
    methods.put(1, new int[]{1});

    listener.testFinished("A", "B", classes, methods);
    listener.testFinished("B", "A", classes, methods);


    listener.testsFinished();
    assertThat(baos.toByteArray()).isEqualTo(twoTestsIncrementalDict);
  }
}