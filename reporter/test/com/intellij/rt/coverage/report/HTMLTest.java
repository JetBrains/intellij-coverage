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

import com.intellij.rt.coverage.report.data.BinaryReport;
import com.intellij.rt.coverage.report.util.FileUtils;
import com.intellij.rt.coverage.util.ProcessUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class HTMLTest {
  private static final String DEFAULT_TITLE = "TITLE";

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
    final File htmlDir = runTestAndConvertToHTML("testData.outOfPackageStructure\\..*", "testData.outOfPackageStructure.TestOutOfPackageStructureKt");
    verifyHTMLDir(htmlDir);
    final File sourcesFile = new File(htmlDir, TestUtils.join("ns-1", "sources", "source-1.html"));
    Assert.assertTrue(sourcesFile.exists());
    Assert.assertFalse(FileUtils.readAll(sourcesFile).contains("Source code is not available"));
    Assert.assertTrue(FileUtils.readAll(sourcesFile).contains("package testData.outOfPackageStructure"));
  }

  @Test
  public void testTopLevel() throws Throwable {
    final File htmlDir = runTestAndConvertToHTML("-exclude testData.*", "TestTopLevelKt");
    verifyHTMLDir(htmlDir);
    final File sourcesFile = new File(htmlDir, TestUtils.join("ns-1", "sources", "source-1.html"));
    Assert.assertTrue(sourcesFile.exists());
    Assert.assertFalse(FileUtils.readAll(sourcesFile).contains("Source code is not available"));
    Assert.assertTrue(FileUtils.readAll(sourcesFile).contains("fun main() {"));
  }

  @Test
  public void integrationTest() throws Throwable {
    final BinaryReport report = TestUtils.runTest("testData.simple.*", "testData.simple.Main");
    final File htmlDir = createHtmlDir(report.getDataFile());
    final File argsFile = ReporterArgsTest.argsToFile(report, TestUtils.JAVA_OUTPUT, "test", null, htmlDir.getAbsolutePath(), "testData.simple.*", "raw", DEFAULT_TITLE);

    final String[] commandLine = {
        "-classpath", System.getProperty("java.class.path"),
        "com.intellij.rt.coverage.report.Main",
        argsFile.getAbsolutePath()};
    ProcessUtil.execJavaProcess(commandLine);
    verifyHTMLDir(htmlDir);
  }

  public static void verifyHTMLDir(File htmlDir) throws IOException {
    Assert.assertTrue(htmlDir.exists());
    Assert.assertTrue(htmlDir.isDirectory());
    final File[] children = htmlDir.listFiles();
    Assert.assertNotNull(children);
    Assert.assertTrue(children.length > 0);
    final File indexFile = new File(htmlDir, "index.html");
    Assert.assertTrue(indexFile.exists());
    final String content = FileUtils.readAll(indexFile);
    Assert.assertTrue(content.contains("<title>" + DEFAULT_TITLE + " Coverage Report > Summary</title>"));
    Assert.assertTrue(content.contains("<h1>" + DEFAULT_TITLE + ": Overall Coverage Summary </h1>"));
    Assert.assertTrue(content.contains("Current scope: " + DEFAULT_TITLE + "<span class=\"separator\">|</span>    all classes"));

    Assert.assertTrue(new File(htmlDir, "ns-1").exists());
  }

  private File runTestAndConvertToHTML(String patterns, String className) throws Throwable {
    final BinaryReport report = TestUtils.runTest(patterns, className);
    final File htmlDir = createHtmlDir(report.getDataFile());
    TestUtils.createRawReporter(report, patterns).createHTMLReport(htmlDir, DEFAULT_TITLE);
    return htmlDir;
  }

  @NotNull
  public static File createHtmlDir(File icFile) {
    final String dirName = icFile.getName().replace(".ic", "html");
    final File htmlDir = new File(icFile.getParentFile(), dirName);
    htmlDir.mkdir();
    return htmlDir;
  }
}
