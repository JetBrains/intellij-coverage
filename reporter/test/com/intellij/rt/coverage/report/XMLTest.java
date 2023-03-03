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

import com.intellij.rt.coverage.data.*;
import com.intellij.rt.coverage.data.instructions.ClassInstructions;
import com.intellij.rt.coverage.data.instructions.LineInstructions;
import com.intellij.rt.coverage.report.data.BinaryReport;
import com.intellij.rt.coverage.report.util.FileUtils;
import com.intellij.rt.coverage.util.ProcessUtil;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class XMLTest {

  private void test(String testName) throws Throwable {
    final String patterns = "testData\\." + testName + "\\..*";
    final String className = "testData." + testName + ".TestKt";
    final String expectedFileName = "xml/" + testName + ".xml";

    verifyXML(patterns, className, expectedFileName);
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
  public void testDeprecated() throws Throwable {
    test("deprecated");
  }

  @Test
  public void testEmptyMethod() throws Throwable {
    test("emptyMethod");
  }

  @Test
  public void testExcludeAnnotation() throws Throwable {
    final String patterns = "testData.excludeAnnotation.* -excludeAnnotations testData.excludeAnnotation.ExcludeFromCoverage";
    final String className = "testData.excludeAnnotation.TestKt";
    verifyXML(patterns, className, "xml/excludeAnnotation.xml");
  }

  @Test
  public void testInline() throws Throwable {
    test("inline");
  }

  @Test
  public void testNoReport() throws Throwable {
    final File xmlFile = createXMLFile();
    TestUtils.createRawReporter(null, "testData.noReport.*").createXMLReport(xmlFile);
    verifyXMLWithExpected(xmlFile, "xml/noReport.xml");
  }

  @Test
  public void testSimple() throws Throwable {
    final String patterns = "testData.simple.*";
    final String className = "testData.simple.Main";
    verifyXML(patterns, className, "xml/simple.xml");
  }

  @Test
  public void integrationTest() throws Throwable {
    final BinaryReport report = TestUtils.runTest("testData.simple.*", "testData.simple.Main");
    final File xmlFile = createXMLFile();
    final File argsFile = ReporterArgsTest.argsToFile(report, TestUtils.JAVA_OUTPUT, "test", xmlFile.getAbsolutePath(), null, "testData.simple.*", "raw", null);

    final String[] commandLine = {
        "-classpath", System.getProperty("java.class.path"),
        "com.intellij.rt.coverage.report.Main",
        argsFile.getAbsolutePath()};
    TestUtils.clearLogFile(new File("."));
    ProcessUtil.execJavaProcess(commandLine);
    TestUtils.checkLogFile(new File("."));
    XMLTest.verifyXMLWithExpected(xmlFile, "xml/simple.xml");
  }

  @Test
  public void integrationWithAggregatorTest() throws Throwable {
    final String patterns = "testData.simple.*";
    final BinaryReport report = TestUtils.runTest(patterns, "testData.simple.Main");

    final File smapFile = new File(report.getDataFile().getAbsolutePath() + ".sm");
    final BinaryReport aggregatedReport = new BinaryReport(report.getDataFile(), smapFile);
    TestUtils.runAggregator(aggregatedReport, patterns);

    final File xmlFile = createXMLFile();
    final File argsFile = ReporterArgsTest.argsToFile(aggregatedReport, TestUtils.JAVA_OUTPUT, "test", xmlFile.getAbsolutePath(), null, patterns, "kover-agg", null);

    final String[] commandLine = {
        "-classpath", System.getProperty("java.class.path"),
        "com.intellij.rt.coverage.report.Main",
        argsFile.getAbsolutePath()};
    TestUtils.clearLogFile(new File("."));
    ProcessUtil.execJavaProcess(commandLine);
    TestUtils.checkLogFile(new File("."));
    verifyXMLWithExpected(xmlFile, "xml/simple.xml");
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
    TestUtils.clearLogFile(new File("."));
    new XMLCoverageReport().write(new FileOutputStream(file), project);
    TestUtils.checkLogFile(new File("."));
    verifyXMLWithExpected(file, "xml/xmlTest.xml");
  }

  @Test
  public void sameFileNameTest() throws Throwable {
    final ProjectData project = new ProjectData();
    project.setInstructionsCoverage(true);
    final Map<String, ClassInstructions> projectInstructions = project.getInstructions();

    final ClassData classData1 = project.getOrCreateClassData("package.A");
    final LineData lineData1 = new LineData(1, "foo()V");
    lineData1.setHits(1);
    final LineData[] lines1 = new LineData[]{null, lineData1};
    classData1.setLines(lines1);
    classData1.setSource("A.kt");

    final LineInstructions lineInstructions1 = new LineInstructions();
    lineInstructions1.setInstructions(1);
    projectInstructions.put("package.A", new ClassInstructions(new LineInstructions[]{null, lineInstructions1}));

    final ClassData classData2 = project.getOrCreateClassData("package.B");
    final LineData lineData2 = new LineData(1, "foo()V");
    lineData2.setHits(1);
    final LineData[] lines2 = new LineData[]{null, lineData2};
    classData2.setLines(lines2);
    classData2.setSource("A.kt");

    final LineInstructions lineInstructions2 = new LineInstructions();
    lineInstructions2.setInstructions(1);
    projectInstructions.put("package.B", new ClassInstructions(new LineInstructions[]{null, lineInstructions2}));

    final File file = createXMLFile();
    TestUtils.clearLogFile(new File("."));
    new XMLCoverageReport().write(new FileOutputStream(file), project);
    TestUtils.checkLogFile(new File("."));
    verifyXMLWithExpected(file, "xml/sameSource.xml");
  }

  @Test
  public void testXMLRead() throws Throwable {
    final InputStream inputStream = TestUtils.class.getClassLoader().getResourceAsStream("xml/simple.xml");
    final XMLProjectData report = new XMLCoverageReport().read(inputStream);

    Assert.assertEquals(1, report.getClasses().size());
    XMLProjectData.ClassInfo classInfo = report.getClass("testData.simple.Main");
    Assert.assertNotNull(classInfo);
    Assert.assertEquals(22, classInfo.missedInstructions);
    Assert.assertEquals(17, classInfo.coveredInstructions);
    Assert.assertEquals(3, classInfo.missedBranches);
    Assert.assertEquals(2, classInfo.coveredBranches);
    Assert.assertEquals(5, classInfo.missedLines);
    Assert.assertEquals(6, classInfo.coveredLines);
    Assert.assertEquals(1, classInfo.missedMethods);
    Assert.assertEquals(1, classInfo.coveredMethods);

    Assert.assertEquals(1, report.getFiles().size());
    XMLProjectData.FileInfo fileInfo = report.getFile("testData/simple/Main.java");
    Assert.assertNotNull(fileInfo);
    Assert.assertEquals(11, fileInfo.lines.size());
    XMLProjectData.LineInfo lineInfo = fileInfo.lines.get(0);
    Assert.assertEquals(19, lineInfo.lineNumber);
    Assert.assertEquals(2, lineInfo.missedInstructions);
    Assert.assertEquals(0, lineInfo.coveredInstructions);
    Assert.assertEquals(0, lineInfo.missedBranches);
    Assert.assertEquals(0, lineInfo.coveredBranches);
  }

  private static void verifyXMLRead(File xmlReport, ProjectData expected) throws Throwable {
    Assert.assertTrue(XMLCoverageReport.canReadFile(xmlReport));
    XMLProjectData actual = new XMLCoverageReport().read(new FileInputStream(xmlReport));
    int classCount = 0;
    Map<String, Map<Integer, XMLProjectData.LineInfo>> files = new HashMap<String, Map<Integer, XMLProjectData.LineInfo>>();
    for (ClassData classData : expected.getClassesCollection()) {
      if (!hasLines(classData.getLines())) continue;
      classCount++;
      XMLProjectData.ClassInfo classInfo = actual.getClass(classData.getName());
      Assert.assertNotNull(classInfo);
      Map<String, Boolean> methods = new HashMap<String, Boolean>();

      Assert.assertNotNull(classData.getSource());
      Assert.assertEquals(classData.getSource(), classInfo.fileName);
      int index = classData.getName().lastIndexOf('.');
      String packageName = index < 0 ? "" : classData.getName().substring(0, index);
      String path = packageName.isEmpty() ? classData.getSource() : packageName.replace('.', '/') + "/" + classData.getSource();
      Map<Integer, XMLProjectData.LineInfo> fileLines = files.get(path);
      if (fileLines == null) {
        fileLines = new HashMap<Integer, XMLProjectData.LineInfo>();
        files.put(path, fileLines);
      }
      int mi = 0, ci = 0, mb = 0, cb = 0, mm = 0, cm = 0, ml = 0, cl = 0;
      for (LineData line : (LineData[]) classData.getLines()) {
        if (line == null) continue;

        XMLProjectData.LineInfo lineInfo = fileLines.get(line.getLineNumber());
        if (lineInfo == null) {
          lineInfo = new XMLProjectData.LineInfo(line.getLineNumber());
          fileLines.put(line.getLineNumber(), lineInfo);
        }
        final BranchData branchData = line.getBranchData();
        if (branchData != null) {
          lineInfo.coveredBranches += branchData.getCoveredBranches();
          lineInfo.missedBranches += branchData.getTotalBranches() - branchData.getCoveredBranches();
        }

        int i = expected.getInstructions().get(classData.getName()).getlines()[line.getLineNumber()].getInstructions();
        if (line.getStatus() == LineCoverage.NONE) {
          ml++;
          lineInfo.missedInstructions += i;
          if (!methods.containsKey(line.getMethodSignature())) {
            methods.put(line.getMethodSignature(), false);
          }
        } else {
          cl++;
          lineInfo.coveredInstructions += i;
          methods.put(line.getMethodSignature(), true);
        }
        ci += lineInfo.coveredInstructions;
        mi += lineInfo.missedInstructions;
        cb += lineInfo.coveredBranches;
        mb += lineInfo.missedBranches;
      }
      for (Map.Entry<String, Boolean> e : methods.entrySet()) {
        if (e.getValue()) {
          cm++;
        } else {
          mm++;
        }
      }
      Assert.assertEquals(mi, classInfo.missedInstructions);
      Assert.assertEquals(ci, classInfo.coveredInstructions);
      Assert.assertEquals(mb, classInfo.missedBranches);
      Assert.assertEquals(cb, classInfo.coveredBranches);
      Assert.assertEquals(ml, classInfo.missedLines);
      Assert.assertEquals(cl, classInfo.coveredLines);
      Assert.assertEquals(mm, classInfo.missedMethods);
      Assert.assertEquals(cm, classInfo.coveredMethods);
    }
    Assert.assertEquals(classCount, actual.getClasses().size());
    Assert.assertEquals(files.size(), actual.getFiles().size());
    for (XMLProjectData.FileInfo fileInfo : actual.getFiles()) {
      Map<Integer, XMLProjectData.LineInfo> expectedFile = files.get(fileInfo.path);
      Assert.assertNotNull(expectedFile);
      for (XMLProjectData.LineInfo lineInfo : fileInfo.lines) {
        XMLProjectData.LineInfo expectedLine = expectedFile.get(lineInfo.lineNumber);
        Assert.assertNotNull(expectedLine);
        Assert.assertEquals(expectedLine.coveredBranches, lineInfo.coveredBranches);
        Assert.assertEquals(expectedLine.missedBranches, lineInfo.missedBranches);
        Assert.assertEquals(expectedLine.coveredInstructions, lineInfo.coveredInstructions);
        Assert.assertEquals(expectedLine.missedInstructions, lineInfo.missedInstructions);
      }
    }
  }

  private static boolean hasLines(Object[] lines) {
    if (lines == null) return false;
    for (Object line : lines) {
      if (line != null) return true;
    }
    return false;
  }

  private static Pair<File, ProjectData> runXMLTest(String patterns, String className) throws Throwable {
    final BinaryReport report = TestUtils.runTest(patterns, className);
    final File xmlFile = createXMLFile();
    TestUtils.clearLogFile(new File("."));
    final Reporter reporter = TestUtils.createReporter(report, patterns);
    reporter.createXMLReport(xmlFile);
    TestUtils.checkLogFile(new File("."));
    return new Pair<File, ProjectData>(xmlFile, reporter.getProjectData());
  }

  @NotNull
  public static File createXMLFile() throws IOException {
    return File.createTempFile("report_tmp", ".xml");
  }

  public static void verifyXMLWithExpected(File file, String expectedFileName) throws Throwable {
    final File expected = TestUtils.getResourceFile(expectedFileName);
    Assert.assertEquals(FileUtils.readAll(expected), FileUtils.readAll(file));
  }

  private static void verifyXML(String patterns, String className, String expectedFileName) throws Throwable {
    Pair<File, ProjectData> result = runXMLTest(patterns, className);

    verifyXMLWithExpected(result.getFirst(), expectedFileName);
    verifyXMLRead(result.getFirst(), result.getSecond());
  }
}
