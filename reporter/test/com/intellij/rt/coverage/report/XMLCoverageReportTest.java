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
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class XMLCoverageReportTest {
  private ProjectData createProject() {
    ProjectData project = new ProjectData();
    ClassData classData1 = project.getOrCreateClassData("MyClass");
    ClassData classData2 = project.getOrCreateClassData("package.MyClass2");
    LineData[] lines1 = new LineData[]{
        new LineData(1, "foo(I)V"),
        new LineData(2, "foo(I)V"),
        new LineData(3, "boo()V")
    };

    LineData[] lines2 = new LineData[]{
        new LineData(1, "a(II)I"),
        new LineData(2, "b(J)Z")
    };
    classData1.setLines(lines1);
    classData2.setLines(lines2);

    classData1.setSource("F.java");
    classData2.setSource("A.java");
    return project;
  }

  @Test
  public void testVerifyXML() throws Throwable {
    File file = createXMLFile();
    new XMLCoverageReport().write(new FileOutputStream(file), createProject());
    verifyProjectXML(file, "xmlTest.xml");
  }

  @Test
  public void integrationTestVerifyXML() throws Throwable {
    verifyProjectXML(runTestAndConvertToXML(".*", "testData.simple.Main"), "xmlIntegrationTest.xml");
  }

  @Test
  public void integrationTestInline() throws Throwable {
    verifyProjectXML(runTestAndConvertToXML("testData\\.inline\\..*", "testData.inline.Test"), "inlineIntegrationTest.xml");
  }

  @Test
  public void integrationTestBranches() throws Throwable {
    verifyProjectXML(runTestAndConvertToXML("testData\\.branches\\..*", "testData.branches.TestKt", true), "branches.xml");
  }

  @Test
  public void integrationTestCrossInline() throws Throwable {
    verifyProjectXML(runTestAndConvertToXML("testData\\.crossinline\\..*", "testData.crossinline.TestKt", true), "crossinline.xml");
  }

  @Test
  public void integrationTestNoReport() throws Throwable {
    final File xmlFile = createXMLFile();
    final String output = "build" + File.separator + "classes" + File.separator + "kotlin" + File.separator + "test" + File.separator + "testData" + File.separator + "noReport";
    TestUtils.createReporter(null, output, null).createXMLReport(xmlFile);
    verifyProjectXML(xmlFile, "xmlNoReport.xml");
  }

  private File runTestAndConvertToXML(String patterns, String className) throws Throwable {
    return runTestAndConvertToXML(patterns, className, false);
  }

  private File runTestAndConvertToXML(String patterns, String className, boolean calcUnloaded) throws Throwable {
    final BinaryReport report = TestUtils.runTest(patterns, className, calcUnloaded);
    final File xmlFile = createXMLFile();
    TestUtils.createReporter(report, null, null).createXMLReport(xmlFile);
    return xmlFile;
  }

  @NotNull
  public static File createXMLFile() throws IOException {
    return File.createTempFile("report_tmp", ".xml");
  }

  public static void verifyProjectXML(File file, String expectedFileName) throws Throwable {
    File expected = TestUtils.getResourceFile(expectedFileName);
    Assert.assertEquals(FileUtils.readAll(expected), FileUtils.readAll(file));
  }
}
