/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

package com.intellij.rt.coverage.util;

import com.intellij.rt.coverage.CoverageStatusTest;
import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Scanner;

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
    File file = File.createTempFile("report_tmp", ".xml");
    new XMLCoverageReport().write(new FileOutputStream(file), createProject());
    verifyProjectXML(file, "xmlTest.xml");
  }

  @Test
  public void integrationTestVerifyXML() throws Throwable {
    try {
      File file = File.createTempFile("report_tmp", ".xml");
      CoverageStatusTest.runCoverage(System.getProperty("java.class.path"), file, "-xml .*", "testData.Main", false);
      verifyProjectXML(file, "xmlIntegrationTest.xml");
    } finally {
      // xml cannot be parsed to load project
      new File("coverage-error.log").delete();
    }
  }

  private void verifyProjectXML(File file, String expectedFileName) throws Throwable {
    String expectedPath = getClass().getClassLoader().getResource(expectedFileName).getPath();
    File expected = new File(expectedPath);
    Assert.assertEquals(readAll(expected), readAll(file));
  }

  private String readAll(File file) throws Throwable {
    StringBuilder fileContents = new StringBuilder((int) file.length());
    Scanner scanner = null;
    try {
      scanner = new Scanner(file);
      while (scanner.hasNextLine()) {
        fileContents
            .append(scanner.nextLine())
            .append("\n");
      }
      return fileContents.toString();
    } finally {
      if (scanner != null) {
        scanner.close();
      }
    }
  }
}
