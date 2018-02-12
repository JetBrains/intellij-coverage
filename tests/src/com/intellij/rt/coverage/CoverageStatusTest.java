/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.rt.coverage;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import com.intellij.rt.coverage.util.ResourceUtil;
import com.sun.tools.javac.Main;
import junit.framework.TestCase;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author anna
 * @since 22-May-2008
 */
public class CoverageStatusTest extends TestCase {
  private File myDataFile;
  private File myClassFile;

  @Override
  protected void tearDown() throws Exception {
    if (myDataFile != null) myDataFile.delete();
    if (myClassFile != null) myClassFile.delete();
    super.tearDown();
  }

  public void testSimple() throws Exception {
    doTest("simple", "1:NONE\n" +
            "3:FULL\n" +
            "4:PARTIAL\n" +
            "5:FULL\n" +
            "6:FULL\n" +
            "8:NONE\n" +
            "11:PARTIAL\n" +
            "13:FULL\n" +
            "14:FULL\n" +
            "16:NONE\n");
  }

  public void testStaticFieldInInterface() throws Exception {
    String expected = Double.parseDouble(System.getProperty("java.specification.version")) < 1.8 ?
        "1:FULL\n" +
            "4:PARTIAL\n" +
            "5:FULL\n" +
            "6:FULL\n" +
            "7:NONE\n" +
            "8:NONE\n" +
            "11:PARTIAL\n" +
            "13:NONE\n" +
            "14:NONE\n" +
            "16:FULL\n" +
            "18:FULL\n" +
            "19:FULL\n" +
            "24:FULL\n" +
            "29:FULL\n" +
            "30:FULL\n" +
            "34:FULL\n" :
        "1:FULL\n" +
            "4:PARTIAL\n" +
            "5:FULL\n" +
            "6:FULL\n" +
            "7:NONE\n" +
            "8:NONE\n" +
            "11:PARTIAL\n" +
            "13:NONE\n" +
            "14:NONE\n" +
            "16:FULL\n" +
            "18:FULL\n" +
            "19:FULL\n" +
            "23:FULL\n" +
            "24:FULL\n" +
            "28:FULL\n" +
            "29:FULL\n" +
            "30:FULL\n" +
            "34:FULL\n";
    doTest("staticFieldInInterface", expected);
  }

  public void testNotExpressions() throws Exception {
    doTest("notExpressions", "1:FULL\n" +
            "3:FULL\n" +
            "7:FULL\n" +
            "8:FULL\n" +
            "9:FULL\n" +
            "10:FULL\n" +
            "11:FULL\n" +
            "12:FULL\n");
  }

  public void testBranches() throws Exception {
    doTest("branches", "1:NONE\n" +
            "3:FULL\n" +
            "4:PARTIAL\n" +
            "5:PARTIAL\n" +
            "6:NONE\n");
  }

  public void _testLambda() throws Exception {
    doTest("lambda", "1:NONE\n" +
            "3:FULL\n" +
            "4:FULL\n" +
            "5:FULL\n" +
            "10:FULL\n");
  }

  public void testLongClass() throws Exception {
    StringBuilder expectedBuilder = new StringBuilder("1:NONE\n" +
        "3:FULL\n");
    for (int line = 32004; line <= 34004; line++) {
      expectedBuilder.append(line).append(":FULL\n");
    }
    doTest("longClass", expectedBuilder.toString(), true);
  }

  public void testLongClassTracing() throws Exception {
    StringBuilder expectedBuilder = new StringBuilder("1:NONE\n" +
        "3:FULL\n");
    for (int line = 32004; line < 34004; line++) {
      expectedBuilder.append(line).append(":FULL\n");
    }
    doTest("longClass", expectedBuilder.toString());
  }

  private void doTest(final String className, String expected) throws Exception {
    doTest(className, expected, false);
  }

  private void doTest(final String className, String expected, boolean sampling) throws Exception {
    final String testDataPath = new File("").getAbsolutePath() + File.separator + "testData" + File.separator + "coverage" + File.separator + className;

    myDataFile = new File(testDataPath +File.separator+ "Test.ic");

    if (Main.compile(new String[]{testDataPath + File.separator + "Test.java"}) != 0) {
      throw new RuntimeException("Compilation failed");
    }

    myClassFile = new File(testDataPath +File.separator + "Test.class");

    final ProjectData projectInfo = runCoverage(testDataPath, myDataFile, "Test(\\$.*)*", "Test", sampling);

    final StringBuilder buf = new StringBuilder();

    final ClassData classInfo = projectInfo.getClassData("Test");

    assert classInfo != null;

    final Object[] objects = classInfo.getLines();
    final ArrayList<LineData> lines = new ArrayList<LineData>();
    for (Object object : objects) {
      if (object != null) {
        lines.add((LineData)object);
      }
    }
    Collections.sort(lines, new Comparator<LineData>() {
      public int compare(final LineData l1, final LineData l2) {
        return l1.getLineNumber() - l2.getLineNumber();
      }
    });
    for (LineData info : lines) {
      buf.append(info.getLineNumber()).append(":").append(info.getStatus() == 0 ? "NONE" : info.getStatus() == 1 ? "PARTIAL" : "FULL").append("\n");
    }

    assertEquals(expected, buf.toString());
  }

  static ProjectData runCoverage(String testDataPath, File coverageDataFile, final String patterns,
                                 String classToRun, final boolean sampling) throws IOException, InterruptedException {
    String javaHome = System.getenv("JAVA_HOME");
    if (javaHome == null) {
      throw new RuntimeException("JAVA_HOME environment variable needs to be set");
    }
    final String exePath = javaHome + File.separator + "bin" + File.separator + "java";

    final String coverageAgentPath = ResourceUtil.getResourceRoot(ProjectData.class);
    assertThat(coverageAgentPath).isNotNull().matches(".*/coverage-agent(-[0-9.]+)?\\.jar");

    String[] commandLine = {
        exePath,
//        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5007",
        "-javaagent:" + coverageAgentPath + "=\"" + coverageDataFile.getPath() + "\" false false false "
            + sampling + " " + patterns,
        "-classpath", testDataPath, classToRun};
    StringBuffer cmd = new StringBuffer();
    for (String s : commandLine) {
      cmd.append(s).append(" ");
    }
    System.out.println(cmd);

    final Process process = Runtime.getRuntime().exec(commandLine);
    process.waitFor();

    if (process.exitValue() != 0) {
      printStdout(process);
      process.destroy();
      throw new RuntimeException("Exit code != 0");
    }

    process.destroy();

    int retries = 0;
    while (!coverageDataFile.exists()) {
      Thread.sleep(1000);
      retries++;
      if (retries > 10) {
        throw new RuntimeException("Timeout waiting for coverage data file to be created");
      }
    }
    final ProjectData projectInfo = ProjectDataLoader.load(coverageDataFile);
    assert projectInfo != null;
    return projectInfo;
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