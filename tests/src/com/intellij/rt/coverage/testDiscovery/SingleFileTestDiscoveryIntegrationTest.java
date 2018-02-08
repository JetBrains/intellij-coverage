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

import com.intellij.rt.coverage.data.SingleTrFileReader;
import com.intellij.rt.coverage.data.TestDiscoveryProjectData;
import com.intellij.rt.coverage.util.FileUtil;
import com.intellij.rt.coverage.util.ResourceUtil;
import com.intellij.rt.coverage.util.StringUtil;
import com.sun.tools.javac.Main;
import junit.framework.TestCase;
import org.hamcrest.Matcher;
import org.jetbrains.annotations.NotNull;
import org.junit.runner.JUnitLauncher;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SingleFileTestDiscoveryIntegrationTest extends TestCase {
  private List<File> myToRemove = new ArrayList<File>();

  public void testSimple() throws Exception {
//    final File result = doTest("simple", "-Dorg.jetbrains.instrumentation.trace.file.version=2");
    final File result = doTest("simple");
    final MySingleTrFileReader reader = new MySingleTrFileReader(result);
    reader.read();
    final List<String[]> data = reader.data;
    assertThat(data).isNotEmpty();
    assertThat(data).contains(
        new String[]{"Test.test1", "ClassA", "method1"},
        new String[]{"Test.test1", "ClassA", "method2"},

        new String[]{"Test.test2", "ClassB", "method1"},
        new String[]{"Test.test2", "ClassB", "method2"},

        new String[]{"Test.test3", "ClassA", "methodR"},
        new String[]{"Test.test3", "ClassA", "method1"},
        new String[]{"Test.test3", "ClassA", "method2"},
        new String[]{"Test.test3", "ClassB", "methodR"},
        new String[]{"Test.test3", "ClassB", "method1"},
        new String[]{"Test.test3", "ClassB", "method2"}
    );
  }


  private static class MySingleTrFileReader extends SingleTrFileReader.Sequential {
    List<String[]> data;

    MySingleTrFileReader(File file) {
      super(file);
      data = new ArrayList<String[]>(0);
    }

    @Override
    protected void processData(String testName, String className, String methodName) {
      data.add(new String[]{testName, className, methodName});
    }
  }

  @Override
  protected void tearDown() throws Exception {
    for (File file : myToRemove) {
      FileUtil.delete(file);
    }
    myToRemove.clear();
    super.tearDown();
  }

  @NotNull
  private static File getTestData(@NotNull String directory) {
    final File file = new File(new File("").getAbsoluteFile(), "testData/testDiscovery/" + directory);
    assertThat(file).exists().isDirectory();
    return file;
  }

  private File doTest(final String directory, String... additionalJavaOptions) throws Exception {
    final File testData = getTestData(directory);
    final File generated = createTempDir();
    myToRemove.add(generated);

    File dataFile = new File(generated, "td.tr");
    assertThat(dataFile).doesNotExist();

    final File[] sources = testData.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.endsWith(".java");
      }
    });
    assertThat(sources).isNotNull().isNotEmpty();

    final ArrayList<String> args = new ArrayList<String>();
    args.add("-d");
    args.add(generated.getAbsolutePath());
    args.add("-cp");
    args.add(ResourceUtil.getResourceRoot(TestCase.class)); // junit
    for (File source : sources) {
      args.add(source.getAbsolutePath());
    }

    if (Main.compile(args.toArray(new String[0])) != 0) {
      fail("Compilation failed");
    }


    runTestDiscovery(generated.getAbsolutePath(), dataFile, "Test", Arrays.asList(additionalJavaOptions));

    return dataFile;
  }

  @NotNull
  private File createTempDir() throws IOException {
    final File file = File.createTempFile("test-discovery-", "-dir");
    assertTrue(file.delete());
    assertTrue(file.mkdirs());
    file.deleteOnExit();
    return file;
  }

  static void runTestDiscovery(String testDataPath, File traceFile, String testClass, List<String> additionalJavaOptions) throws IOException, InterruptedException {
    String javaHome = System.getenv("JAVA_HOME");
    if (javaHome == null) {
      fail("JAVA_HOME environment variable needs to be set");
    }
    final String exePath = javaHome + File.separator + "bin" + File.separator + "java";

    final String agentJar = ResourceUtil.getResourceRoot(TestDiscoveryProjectData.class);
    assertThat(agentJar).overridingErrorMessage("TestDiscovery agent not found. Please rebuild all artifacts to build it.")
        .isNotNull().endsWith("test-discovery-agent.jar");

    final ArrayList<String> args = new ArrayList<String>();
    args.add(exePath);
    // args.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5007");
    args.add("-javaagent:" + agentJar);
    args.add("-classpath");
    args.add(StringUtil.join(":", testDataPath,
        ResourceUtil.getResourceRoot(JUnitLauncher.class),
        ResourceUtil.getResourceRoot(TestCase.class),
        ResourceUtil.getResourceRoot(Matcher.class)));
    args.add("-Dtest.discovery.data.listener=com.intellij.rt.coverage.data.SingleTrFileDiscoveryDataListener");
    args.add("-Dorg.jetbrains.instrumentation.trace.file=" + traceFile.getAbsolutePath());
    args.addAll(additionalJavaOptions);
    args.add("org.junit.runner.JUnitLauncher");
    args.add(testClass);

    final String[] argsArray = args.toArray(new String[0]);
    System.out.println(StringUtil.join(" ", argsArray));

    final Process process = Runtime.getRuntime().exec(argsArray);
    process.waitFor();

    printStdout(process);
    if (process.exitValue() != 0) {
      process.destroy();
      fail("Exit code != 0");
    }
    process.destroy();

    int retries = 0;
    while (!traceFile.exists()) {
      Thread.sleep(1000);
      retries++;
      if (retries > 10) {
        throw new RuntimeException("Timeout waiting for coverage data file to be created");
      }
    }
  }

  private static void printStdout(Process process) throws IOException {
    BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
    BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    String str;

    while ((str = output.readLine()) != null) {
      System.out.println(str);
    }

    while ((str = error.readLine()) != null) {
      System.out.println(str);
    }
  }

}
