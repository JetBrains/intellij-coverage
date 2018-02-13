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

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SingleTrFileDiscoveryDataListenerTest {

  public static final byte[] EMPTY =
      new BinaryResponseBuilder().withHeader().withStart(1).build();

  public static final byte[] NO_TESTS_ONE_NAME =
      new BinaryResponseBuilder().withHeader().withStart(1)
          .withIncrementalDictionaryStart(1)
          .withDictionaryElement(1, 0x41, 0x42, 0x43) // 1-ABC
          .build();

  public static final byte[] SINGLE_TEST_NO_METHODS =
      new BinaryResponseBuilder().withHeader().withStart(1)
          .withIncrementalDictionaryStart(1)
          .withDictionaryElement(1, 0x41, 0x42, 0x43) // 1-ABC
          .withTestResultStart(1, 1, 0) // Test ABC.ABC, 0 coverage
          .build();

  public static final byte[] SINGLE_TEST_SINGLE_METHOD =
      new BinaryResponseBuilder().withHeader().withStart(1)
          .withIncrementalDictionaryStart(3)
          .withDictionaryElement(1, 0x41) // 1-A
          .withDictionaryElement(2, 0x42) // 2-B
          .withDictionaryElement(3, 0x43) // 3-C
          .withTestResultStart(1, 2, 1) // Test A.B, 1 class
          .withTestResultClass(2, 1) // Class B, 1 method
          .withTestResultMethod(3) // Method C
          .build();

  public static final byte[] TWO_TESTS_INCREMENTAL_DICT =
      new BinaryResponseBuilder().withHeader().withStart(1)
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

  @Test
  public void testSingleTestNothingVisited() throws Exception {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final LongDataOutputStream dos = new LongDataOutputStream(baos);
    final SingleTrFileDiscoveryProtocolDataListener listener = new SingleTrFileDiscoveryProtocolDataListener(dos);
    final String name = "ABC";
    listener.getIncrementalNameEnumerator().enumerate(name);
    final Map<Integer, boolean[]> classes = new HashMap<Integer, boolean[]>();
    final Map<Integer, int[]> methods = new HashMap<Integer, int[]>();
    classes.put(1, new boolean[]{false});
    methods.put(1, new int[]{1});
    listener.testFinished(name, name, classes, methods);
    listener.testsFinished();
    assertThat(baos.toByteArray()).isEqualTo(SINGLE_TEST_NO_METHODS);
  }

  @Test
  public void testV2NoCoverage() throws Exception {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final LongDataOutputStream dos = new LongDataOutputStream(baos);
    final SingleTrFileDiscoveryProtocolDataListener listener = new SingleTrFileDiscoveryProtocolDataListener(dos);
    listener.testsFinished();
    assertThat(baos.toByteArray()).isEqualTo(EMPTY);
  }


  @Test
  public void testV2OnlyEnumerator() throws Exception {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final LongDataOutputStream dos = new LongDataOutputStream(baos);
    final SingleTrFileDiscoveryProtocolDataListener listener = new SingleTrFileDiscoveryProtocolDataListener(dos);
    listener.getIncrementalNameEnumerator().enumerate("ABC");
    listener.testsFinished();
    assertThat(baos.toByteArray()).isEqualTo(NO_TESTS_ONE_NAME);
  }

  @Test
  public void testV2SingleTestOnlyOneClassVisited() throws Exception {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final LongDataOutputStream dos = new LongDataOutputStream(baos);
    final SingleTrFileDiscoveryProtocolDataListener listener = new SingleTrFileDiscoveryProtocolDataListener(dos);

    final String name1 = "A";
    final String name2 = "B";
    final String name3 = "C";
    listener.getIncrementalNameEnumerator().enumerate(name1);
    listener.getIncrementalNameEnumerator().enumerate(name2);
    listener.getIncrementalNameEnumerator().enumerate(name3);

    final Map<Integer, boolean[]> classes = new HashMap<Integer, boolean[]>();
    final Map<Integer, int[]> methods = new HashMap<Integer, int[]>();
    classes.put(1, new boolean[]{false});
    classes.put(2, new boolean[]{true});
    methods.put(1, new int[]{1});
    methods.put(2, new int[]{3});
    listener.testFinished(name1, name2, classes, methods);
    listener.testsFinished();
    assertThat(baos.toByteArray()).isEqualTo(SINGLE_TEST_SINGLE_METHOD);
  }

  @Test
  public void testV2TwoTestsIncrementalDict() throws Exception {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final LongDataOutputStream dos = new LongDataOutputStream(baos);
    final SingleTrFileDiscoveryProtocolDataListener listener = new SingleTrFileDiscoveryProtocolDataListener(dos);

    listener.getIncrementalNameEnumerator().enumerate("A");

    final Map<Integer, boolean[]> classes = new HashMap<Integer, boolean[]>();
    final Map<Integer, int[]> methods = new HashMap<Integer, int[]>();
    classes.put(1, new boolean[]{true});
    methods.put(1, new int[]{1});

    listener.testFinished("A", "B", classes, methods);
    listener.testFinished("B", "A", classes, methods);


    listener.testsFinished();
    assertThat(baos.toByteArray()).isEqualTo(TWO_TESTS_INCREMENTAL_DICT);
  }
}