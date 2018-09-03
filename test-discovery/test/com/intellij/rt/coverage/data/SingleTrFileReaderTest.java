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

import com.intellij.rt.coverage.data.api.SimpleDecodingTestDiscoveryProtocolReader;
import com.intellij.rt.coverage.data.api.TestDiscoveryProtocolUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.runners.Parameterized.*;

@RunWith(Parameterized.class)
public class SingleTrFileReaderTest {

  @Parameters(name = "V{0}")
  public static Object[] versions() {
    return TraceFileVersions.VERSIONS;
  }

  @Parameter
  public int version;

  @Test
  public void testCompletelyEmpty() throws IOException {
    final MySingleTrFileReader reader = read(new byte[0]);
    assertThat(reader.myData).isEmpty();
  }

  @Test
  public void testNoMeaningfulData() throws IOException {
    final MySingleTrFileReader reader = read(BinaryResponses.empty(version));
    assertThat(reader.myData).isEmpty();
  }

  @Test
  public void testOnlyMetadata() throws IOException {
    final MySingleTrFileReader reader = read(BinaryResponses.metadata(version));
    assertThat(reader.myData).isEmpty();
    assertThat(reader.myMetadata).isNotNull().containsOnly(entry("A", "B"));
  }

  @Test
  public void testNoTests() throws IOException {
    final MySingleTrFileReader reader = read(BinaryResponses.noTestsOneName(version));
    assertThat(reader.myData).isEmpty();
  }

  @Test
  public void testOneEmptyTest() throws IOException {
    final MySingleTrFileReader reader = read(BinaryResponses.singleTestNoMethods(version));
    assertThat(reader.myData).isEmpty();
  }

  @Test
  public void testTestWithOneMethod() throws IOException {
    final MySingleTrFileReader reader = read(BinaryResponses.singleTestSingleMethod(version));
    assertThat(reader.myData).isNotEmpty();
    final String[] data = reader.myData.iterator().next();
    assertThat(data).doesNotContainNull().containsExactly("A", "B", "B", "C");
  }

  private MySingleTrFileReader read(byte[] content) throws IOException {
    MySingleTrFileReader reader = new MySingleTrFileReader();
    TestDiscoveryProtocolUtil.readFile(createFileWithContent(content), reader);
    return reader;
  }

  private static File createFileWithContent(byte[] content) throws IOException {
    final File file = File.createTempFile("temporarily-td", ".ijtc");
    final FileOutputStream fos = new FileOutputStream(file);
    fos.write(content);
    fos.close();
    return file;
  }

  private static class MySingleTrFileReader extends SimpleDecodingTestDiscoveryProtocolReader {
    final List<String[]> myData = new ArrayList<String[]>();
    final Map<String, String> myMetadata = new HashMap<String, String>();
    final List<ClassMetadata> myClassMetadata = new ArrayList<ClassMetadata>();

    protected void processData(String testClassName, String testMethodName, String className, String methodName) {
      myData.add(new String[]{testClassName, testMethodName, className, methodName});
    }

    public void processMetadataEntry(String key, String value) {
      myMetadata.put(key, value);
    }

    protected void processClassMetadataData(ClassMetadata metadata) {
      myClassMetadata.add(metadata);
    }
  }
}