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

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class HTMLCoverageReportTest {
  @Test
  public void testSimple() throws Throwable {
    verifyHTMLDir(runTestAndConvertToHTML(".*", "testData.simple.Main", getSources()));
  }

  @Test
  public void testInline() throws Throwable {
    verifyHTMLDir(runTestAndConvertToHTML("testData\\.inline\\..*", "testData.inline.Test", getSources()));
  }

  @Test
  public void testFileOutOfPackageStructure() throws Throwable {
    final File htmlDir = runTestAndConvertToHTML("testData\\..*", "testData.outOfPackageStructure.TestOutOfPackageStructureKt", getSources());
    verifyHTMLDir(htmlDir);
    final File sourcesFile = new File(htmlDir, "ns-1" + File.separator + "sources" + File.separator + "source-1.html");
    Assert.assertTrue(sourcesFile.exists());
    Assert.assertFalse(readFile(sourcesFile).contains("Source code is not available"));
  }

  @Test
  public void testTopLevel() throws Throwable {
    final File htmlDir = runTestAndConvertToHTML("", "TestTopLevelKt", getSources());
    verifyHTMLDir(htmlDir);
    final File sourcesFile = new File(htmlDir, "ns-1" + File.separator + "sources" + File.separator + "source-1.html");
    Assert.assertTrue(sourcesFile.exists());
    Assert.assertFalse(readFile(sourcesFile).contains("Source code is not available"));
  }

  public static void verifyHTMLDir(File htmlDir) {
    Assert.assertTrue(htmlDir.exists());
    Assert.assertTrue(htmlDir.isDirectory());
    final File[] children = htmlDir.listFiles();
    Assert.assertNotNull(children);
    Assert.assertTrue(children.length > 0);
    Assert.assertTrue(new File(htmlDir, "index.html").exists());
  }

  private static String readFile(File file) throws IOException {
    final StringBuilder builder = new StringBuilder();
    final BufferedReader reader = new BufferedReader(new FileReader(file));
    String line;
    while ((line = reader.readLine()) != null) {
      builder.append(line).append("\n");
    }
    return builder.toString();
  }

  private File runTestAndConvertToHTML(String patterns, String className, List<File> sources) throws Throwable {
    final Reporter reporter = TestUtils.runTest(patterns, className);
    final File htmlDir = createHtmlDir(reporter.getReport().getDataFile());
    reporter.createHTMLReport(htmlDir, sources);
    return htmlDir;
  }

  @NotNull
  public static File createHtmlDir(File icFile) {
    final String dirName = icFile.getName().replace(".ic", "html");
    final File htmlDir = new File(icFile.getParentFile(), dirName);
    htmlDir.mkdir();
    return htmlDir;
  }

  @NotNull
  private List<File> getSources() {
    return Collections.singletonList(new File("test"));
  }
}
