/*
 * User: anna
 * Date: 22-May-2008
 */
package com.intellij.rt.coverage;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import com.sun.tools.javac.Main;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class CoverageStatusTest extends TestCase {
  private File myDataFile;
  private File myClassFile;

  @Override
  protected void tearDown() throws Exception {
    myDataFile.delete();
    myClassFile.delete();
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
    doTest("staticFieldInInterface", "1:FULL\n" +
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
            "34:FULL\n");
  }

  public void testNotExpressions() throws Exception {
    doTest("notExpressions", "1:FULL\n" +
            "3:FULL\n" +
            "7:FULL\n" +
            "8:PARTIAL\n" +
            "9:FULL\n" +
            "10:FULL\n" +
            "11:PARTIAL\n" +
            "12:FULL\n");
  }

  public void testBranches() throws Exception {
    doTest("branches", "1:NONE\n" +
            "3:FULL\n" +
            "4:PARTIAL\n" +
            "5:PARTIAL\n" +
            "6:NONE\n");
  }

  private void doTest(final String className, String expected) throws Exception {
    final String testDataPath = new File("").getAbsolutePath() + File.separator + "tests" + File.separator + "testData" + File.separator + "coverage" + File.separator + className;

    myDataFile = new File(testDataPath +File.separator+ "Test.ic");

    Main.compile(new String[]{testDataPath + File.separator + "Test.java"});

    myClassFile = new File(testDataPath +File.separator + "Test.class");

    final String exePath = System.getenv("JAVA_HOME") + File.separator + "bin" + File.separator + "java";
    final String path = new File("").getAbsolutePath() + File.separator + "dist" + File.separator;
    final String coverageAgentPath = path + "coverage-agent.jar";
    System.out.println(coverageAgentPath);
    String classpath = testDataPath;
   /*classpath += File.pathSeparator + path + "asm.jar";
    classpath += File.pathSeparator + path + "asm-commons.jar";
    classpath += File.pathSeparator + path + "asm-tree-4.0.jar";*/

    final Process process = Runtime.getRuntime().exec(new String[]{
            exePath,
            "-javaagent:" + coverageAgentPath + "=\"" + myDataFile.getPath() + "\" false false false false Test(\\$.*)*",
            "-classpath", classpath, "Test"});
    process.waitFor();
    process.destroy();

    while (!myDataFile.exists()) {
      Thread.sleep(1000);
    }
    final ProjectData projectInfo = ProjectDataLoader.load(myDataFile);
    assert projectInfo != null;

    final StringBuffer buf = new StringBuffer();

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

    Assert.assertEquals(expected, buf.toString());
  }

}