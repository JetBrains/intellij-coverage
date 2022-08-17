/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package com.intellij.rt.coverage.report;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.report.data.BinaryReport;
import com.intellij.rt.coverage.report.util.FileUtils;
import com.intellij.rt.coverage.util.ProcessUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class XMLTest {

  private void test(String testName) throws Throwable {
    final String patterns = "testData\\." + testName + "\\..*";
    final String className = "testData." + testName + ".TestKt";
    final String expectedFileName = "xml/" + testName + ".xml";
    verifyProjectXML(runTestAndConvertToXML(patterns, className), expectedFileName);
  }

  @Test
  public void testBranches() throws Throwable {
    test("branches");
  }

  @Test
  public void testCrossinline() throws Throwable {
    test("crossinline");
  }

  @Test
  public void testDefaultArgs() throws Throwable {
    test("defaultArgs");
  }

  @Test
  public void testInline() throws Throwable {
    test("inline");
  }

  @Test
  public void testNoReport() throws Throwable {
    final File xmlFile = createXMLFile();
    TestUtils.createReporter(null, "testData.noReport.*").createXMLReport(xmlFile);
    verifyProjectXML(xmlFile, "xml/noReport.xml");
  }

  @Test
  public void testSimple() throws Throwable {
    verifyProjectXML(runTestAndConvertToXML("testData.simple.*", "testData.simple.Main"), "xml/simple.xml");
  }

  @Test
  public void integrationTest() throws Throwable {
    final BinaryReport report = TestUtils.runTest("testData.simple.*", "testData.simple.Main");
    final File xmlFile = createXMLFile();
    final File argsFile = ReporterArgsTest.argsToFile(report, TestUtils.JAVA_OUTPUT, "test", xmlFile.getAbsolutePath(), null, "testData.simple.*");

    final String[] commandLine = {
        "-classpath", System.getProperty("java.class.path"),
        "com.intellij.rt.coverage.report.Main",
        argsFile.getAbsolutePath()};
    ProcessUtil.execJavaProcess(commandLine);
    XMLTest.verifyProjectXML(xmlFile, "xml/simple.xml");
  }

  @Test
  public void basicTest() throws Throwable {
    final ProjectData project = new ProjectData();
    final ClassData classData1 = project.getOrCreateClassData("MyClass");
    final ClassData classData2 = project.getOrCreateClassData("package.MyClass2");
    final LineData[] lines1 = new LineData[]{
        new LineData(1, "foo(I)V"),
        new LineData(2, "foo(I)V"),
        new LineData(3, "boo()V")
    };

    final LineData[] lines2 = new LineData[]{
        new LineData(1, "a(II)I"),
        new LineData(2, "b(J)Z")
    };
    classData1.setLines(lines1);
    classData2.setLines(lines2);

    classData1.setSource("F.java");
    classData2.setSource("A.java");

    final File file = createXMLFile();
    new XMLCoverageReport().write(new FileOutputStream(file), project);
    verifyProjectXML(file, "xml/xmlTest.xml");
  }

  private File runTestAndConvertToXML(String patterns, String className) throws Throwable {
    final BinaryReport report = TestUtils.runTest(patterns, className);
    final File xmlFile = createXMLFile();
    TestUtils.createReporter(report, patterns).createXMLReport(xmlFile);
    return xmlFile;
  }

  @NotNull
  public static File createXMLFile() throws IOException {
    return File.createTempFile("report_tmp", ".xml");
  }

  public static void verifyProjectXML(File file, String expectedFileName) throws Throwable {
    final File expected = TestUtils.getResourceFile(expectedFileName);
    Assert.assertEquals(FileUtils.readAll(expected), FileUtils.readAll(file));
  }
}
