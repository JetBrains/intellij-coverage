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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SingleTrFileReaderTest {

  @Test
  public void testCompletelyEmpty() throws IOException {
    final MySingleTrFileReader reader = getReader(new byte[0]);
    reader.read();
    assertThat(reader.myData).isEmpty();
  }

  @Test
  public void testNoMeaningfulData() throws IOException {
    final MySingleTrFileReader reader = getReader(SingleTrFileDiscoveryDataListenerTest.EMPTY);
    reader.read();
    assertThat(reader.myData).isEmpty();
  }

  @Test
  public void testNoTests() throws IOException {
    final MySingleTrFileReader reader = getReader(SingleTrFileDiscoveryDataListenerTest.NO_TESTS_ONE_NAME);
    reader.read();
    assertThat(reader.myData).isEmpty();
  }

  @Test
  public void testOneEmptyTest() throws IOException {
    final MySingleTrFileReader reader = getReader(SingleTrFileDiscoveryDataListenerTest.SINGLE_TEST_NO_METHODS);
    reader.read();
    assertThat(reader.myData).isEmpty();
  }

  @Test
  public void testTestWithOneMethod() throws IOException {
    final MySingleTrFileReader reader = getReader(SingleTrFileDiscoveryDataListenerTest.SINGLE_TEST_SINGLE_METHOD);
    reader.read();
    assertThat(reader.myData).isNotEmpty();
    final String[] data = reader.myData.iterator().next();
    assertThat(data).doesNotContainNull().containsExactly("A", "B", "C");
  }

  @Test
  public void testV2TestWithOneMethod() throws IOException {
    final MySingleTrFileReader reader = getReader(SingleTrFileDiscoveryDataListenerTest.V2_SINGLE_TEST_SINGLE_METHOD);
    reader.read();
    assertThat(reader.myData).isNotEmpty();
    final String[] data = reader.myData.iterator().next();
    assertThat(data).doesNotContainNull().containsExactly("A", "A", "B");
  }

  private MySingleTrFileReader getReader(byte[] content) throws IOException {
    return new MySingleTrFileReader(SingleTrFileReaderTest.createFileWithContent(content));
  }

  private static File createFileWithContent(byte[] content) throws IOException {
    final File file = File.createTempFile("temporarily-td", ".tr");
    final FileOutputStream fos = new FileOutputStream(file);
    fos.write(content);
    fos.close();
    return file;
  }

  private static class MySingleTrFileReader extends SingleTrFileReader {
    List<String[]> myData;

    MySingleTrFileReader(File file) {
      super(file);
      myData = new ArrayList<String[]>(0);
    }

    @Override
    protected void processData(String testName, String className, String methodName) {
      myData.add(new String[]{testName, className, methodName});
      super.processData(testName, className, methodName);
    }
  }
}