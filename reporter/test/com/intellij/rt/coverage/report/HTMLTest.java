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

import com.intellij.rt.coverage.report.api.Filters;
import com.intellij.rt.coverage.report.api.ReportApi;
import com.intellij.rt.coverage.report.data.BinaryReport;
import com.intellij.rt.coverage.report.util.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;

public class HTMLTest {
  private static final String DEFAULT_TITLE = "TITLE";
  private static final String DEFAULT_CHARSET = "UTF-8";

  @Test
  public void testSimple() throws Throwable {
    verifyHTMLDir(runTestAndConvertToHTML(".*", "testData.simple.Main"));
  }

  @Test
  public void testInline() throws Throwable {
    verifyHTMLDir(runTestAndConvertToHTML("testData\\.inline\\..*", "testData.inline.TestKt"));
  }

  @Test
  public void testFileOutOfPackageStructure() throws Throwable {
    File htmlDir = runTestAndConvertToHTML("testData.outOfPackageStructure\\..*", "testData.outOfPackageStructure.TestOutOfPackageStructureKt");
    verifyHTMLDir(htmlDir);
    File sourcesFile = new File(htmlDir, TestUtils.join("ns-1", "sources", "source-1.html"));
    Assert.assertTrue(sourcesFile.exists());
    Assert.assertFalse(FileUtils.readAll(sourcesFile).contains("Source code is not available"));
    Assert.assertTrue(FileUtils.readAll(sourcesFile).contains("package testData.outOfPackageStructure"));
  }

  @Test
  public void testTopLevel() throws Throwable {
    File htmlDir = runTestAndConvertToHTML("-exclude testData.*", "TestTopLevelKt");
    verifyHTMLDir(htmlDir);
    File sourcesFile = new File(htmlDir, TestUtils.join("ns-1", "sources", "source-1.html"));
    Assert.assertTrue(sourcesFile.exists());
    Assert.assertFalse(FileUtils.readAll(sourcesFile).contains("Source code is not available"));
    Assert.assertTrue(FileUtils.readAll(sourcesFile).contains("fun main() {"));
  }

  @Test
  public void apiTest() throws Throwable {
    BinaryReport report = TestUtils.runTest("testData.simple.*", "testData.simple.Main");
    File htmlDir = createHtmlDir(report.getDataFile());

    Filters filters = TestUtils.createFilters(Pattern.compile("testData.simple.*"));
    ReportApi.htmlReport(htmlDir, DEFAULT_TITLE, null, singletonList(report.getDataFile()), singletonList(new File(TestUtils.JAVA_OUTPUT)), singletonList(new File("test")), filters);

    verifyHTMLDir(htmlDir);
  }

  public static void verifyHTMLDir(File htmlDir) throws IOException {
    Assert.assertTrue(htmlDir.exists());
    Assert.assertTrue(htmlDir.isDirectory());
    File[] children = htmlDir.listFiles();
    Assert.assertNotNull(children);
    Assert.assertTrue(children.length > 0);
    File indexFile = new File(htmlDir, "index.html");
    Assert.assertTrue(indexFile.exists());
    String content = FileUtils.readAll(indexFile);
    Assert.assertTrue(content.contains("<title>" + DEFAULT_TITLE + " Coverage Report > Summary</title>"));
    Assert.assertTrue(content.contains("<h1>" + DEFAULT_TITLE + ": Overall Coverage Summary </h1>"));
    Assert.assertTrue(content.contains("Current scope: " + DEFAULT_TITLE + "<span class=\"separator\">|</span>    all classes"));

    Assert.assertTrue(new File(htmlDir, "ns-1").exists());
  }

  private File runTestAndConvertToHTML(String patterns, String className) throws Throwable {
    BinaryReport report = TestUtils.runTest(patterns, className);
    File htmlDir = createHtmlDir(report.getDataFile());
    TestUtils.clearLogFile(new File("."));
    TestUtils.createRawReporter(report, patterns, DEFAULT_TITLE).createHTMLReport(htmlDir, DEFAULT_CHARSET);
    TestUtils.checkLogFile(new File("."));
    return htmlDir;
  }

  @NotNull
  public static File createHtmlDir(File icFile) {
    String dirName = icFile.getName().replace(".ic", "html");
    File htmlDir = new File(icFile.getParentFile(), dirName);
    htmlDir.mkdir();
    return htmlDir;
  }
}
