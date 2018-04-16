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

package com.intellij.rt.coverage.testDiscovery;

import com.intellij.rt.coverage.data.ClassMetadata;
import com.intellij.rt.coverage.data.api.SimpleDecodingTestDiscoveryProtocolReader;
import com.intellij.rt.coverage.data.api.TestDiscoveryProtocolUtil;
import com.intellij.rt.coverage.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SingleFileTestDiscoveryIntegrationTest {
  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Test
  public void testSimple() throws Exception {
//    final File result = doTest("simple", "-Dorg.jetbrains.instrumentation.trace.file.version=2");
    final File result = doTest("simple");
    MySingleTrFileReader reader = new MySingleTrFileReader();
    TestDiscoveryProtocolUtil.readFile(result, reader);
    final List<String[]> data = reader.data;
    checkClassMeta(reader);
    assertThat(data).isNotEmpty();
    assertThat(data).contains(
        new String[]{"Test", "test1", "Test", "test1/()V"},
        new String[]{"Test", "test1", "ClassA", "method1/()V"},
        new String[]{"Test", "test1", "ClassA", "method2/()V"},

        new String[]{"Test", "test2", "Test", "test2/()V"},
        new String[]{"Test", "test2", "ClassB", "method1/()V"},
        new String[]{"Test", "test2", "ClassB", "method2/()V"},

        new String[]{"Test", "test3", "Test", "test3/()V"},
        new String[]{"Test", "test3", "ClassA", "methodR/()V"},
        new String[]{"Test", "test3", "ClassA", "method1/()V"},
        new String[]{"Test", "test3", "ClassA", "method2/()V"},
        new String[]{"Test", "test3", "ClassB", "methodR/()V"},
        new String[]{"Test", "test3", "ClassB", "method1/()V"},
        new String[]{"Test", "test3", "ClassB", "method2/()V"},
        new String[]{"Test", "testConstructor", "ClassA", "<init>/()V"}
    );
    assertThat(data).doesNotContain(new String[]{"Test.testConstructor", "ClassB", "<init>"});
  }

  @Test
  public void testStaticInitializer() throws Exception {
    final File result = doTest("withStaticInitializers"
        //, "-Dtest.discovery.include.class.patterns=Test;InitClass;A;B", "-Dtest.discovery.exclude.class.patterns=junit.*;org.*"
    );
    MySingleTrFileReader reader = new MySingleTrFileReader();
    TestDiscoveryProtocolUtil.readFile(result, reader);
    final List<String[]> data = reader.data;
    assertThat(data).isNotEmpty();
    assertThat(data).contains(
        new String[]{"Test", "test1", "Test", "test1/()V"},
        new String[]{"Test", "test1", "InitClass", "initInit/()V"}
    );
    checkClassMeta(reader);
  }

  @Test
  public void testInterfaceWithDefaultMethods() throws Exception {
    final File result = doTest("interfaceWithDefaultMethods");
    MySingleTrFileReader reader = new MySingleTrFileReader();
    TestDiscoveryProtocolUtil.readFile(result, reader);
    final List<String[]> data = reader.data;
    assertThat(data).isNotEmpty();
    assertThat(data).contains(
        new String[]{"Test", "test1", "Foo", "m/()V"},
        new String[]{"Test", "test1", "Foo", "doInvoke/()V"}
    );
    checkClassMeta(reader);
  }

  @Test
  public void testSimpleExcludeLibs() throws Exception {
    final File result = doTest("simple",
        "-Dtest.discovery.include.class.patterns=Test.*;Class.*",
        "-Dtest.discovery.exclude.class.patterns=junit.*;org.*");
    MySingleTrFileReader reader = new MySingleTrFileReader();
    TestDiscoveryProtocolUtil.readFile(result, reader);
    final List<String[]> data = reader.data;
    assertThat(data).isNotEmpty();
    assertThat(data).containsOnly(
        new String[]{"Test", "test1", "Test", "test1/()V"},
        new String[]{"Test", "test1", "ClassA", "method1/()V"},
        new String[]{"Test", "test1", "ClassA", "method2/()V"},

        new String[]{"Test", "test2", "Test", "test2/()V"},
        new String[]{"Test", "test2", "ClassB", "method1/()V"},
        new String[]{"Test", "test2", "ClassB", "method2/()V"},

        new String[]{"Test", "test3", "Test", "test3/()V"},
        new String[]{"Test", "test3", "ClassA", "methodR/()V"},
        new String[]{"Test", "test3", "ClassA", "method1/()V"},
        new String[]{"Test", "test3", "ClassA", "method2/()V"},
        new String[]{"Test", "test3", "ClassB", "methodR/()V"},
        new String[]{"Test", "test3", "ClassB", "method1/()V"},
        new String[]{"Test", "test3", "ClassB", "method2/()V"},

        new String[]{"Test", "testConstructor", "ClassA", "<init>/()V"},
        new String[]{"Test", "testConstructor", "ClassA", "someMethod/()V"},
        new String[]{"Test", "testConstructor", "ClassB", "someMethod/()V"},
        new String[]{"Test", "testConstructor", "Test", "testConstructor/()V"}
    );
    checkClassMeta(reader);
  }

  private void checkClassMeta(MySingleTrFileReader reader) {
    assertThat(reader.classMetaData).isNotEmpty();
    for (ClassMetadata classMetadata : reader.classMetaData) {
      assertThat(classMetadata.getFqn()).isNotBlank();
      // for Java:
      assertThat(classMetadata.getFiles()).size().isEqualTo(1);
    }
  }

  private static class MySingleTrFileReader extends SimpleDecodingTestDiscoveryProtocolReader {
    final List<String[]> data = new ArrayList<String[]>();
    final List<ClassMetadata> classMetaData = new ArrayList<ClassMetadata>();

    protected void processData(String testClassName, String testMethodName, String className, String methodName) {
      data.add(new String[]{testClassName, testMethodName, className, methodName});
    }

    public void processMetadataEntry(String key, String value) {

    }

    protected void processClassMetadataData(ClassMetadata metadata) {
      classMetaData.add(metadata);
    }
  }

  @NotNull
  private static File getTestData(@NotNull String directory) {
    final File file = new File(new File("").getAbsoluteFile(), "testData/testDiscovery/" + directory);
    assertThat(file).exists().isDirectory();
    return file;
  }

  private File doTest(final String directory, String... additionalJavaOptions) throws Exception {
    return runTestDiscoveryWithTraceFileOutput(directory, additionalJavaOptions);
  }

  private File runTestDiscoveryWithTraceFileOutput(String directory, String... javaOptions) throws IOException, InterruptedException {
    final File testData = getTestData(directory);
    final File outputDir = tmpDir.newFolder();

    File traceDataFile = tmpDir.newFile("td.ijtc");
    TestDiscoveryTestUtil.compileTestData(testData, outputDir);

    List<String> fullJavaOptions = new ArrayList<String>();
    Collections.addAll(fullJavaOptions, javaOptions);
    // args.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5007");
    fullJavaOptions.add("-Dtest.discovery.data.listener=com.intellij.rt.coverage.data.SingleTrFileDiscoveryProtocolDataListener");
    fullJavaOptions.add("-Dorg.jetbrains.instrumentation.trace.file=" + traceDataFile.getAbsolutePath());

    TestDiscoveryTestUtil.runTestDiscovery(outputDir.getAbsolutePath(), "Test", fullJavaOptions);

    FileUtil.waitUntilFileCreated(traceDataFile);
    return traceDataFile;
  }
}
