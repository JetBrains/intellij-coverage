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

import java.io.File;
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

  public static void verifyHTMLDir(File htmlDir) {
    Assert.assertTrue(htmlDir.exists());
    Assert.assertTrue(htmlDir.isDirectory());
    final File[] children = htmlDir.listFiles();
    Assert.assertNotNull(children);
    Assert.assertTrue(children.length > 0);
    Assert.assertTrue(new File(htmlDir, "index.html").exists());
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
