package com.intellij.rt.coverage;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import com.sun.tools.javac.Main;
import junit.framework.TestCase;

import java.io.File;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Anna.Kozlova
 * Date: 1/19/11
 * Time: 1:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class CoverageMergeTest extends TestCase {
    private static final String COMMON = "Common";
    private Set<File> myFiles2Delete = new HashSet<File>();
    private static final String[] ALL_CLASS_NAMES = new String[]{"Test1", "Test2", COMMON};
    private static final String[] FRST_CLASS_NAMES = new String[]{"Test1", null, COMMON};
    private static final String[] SCD_CLASS_NAMES = new String[]{null, "Test2", COMMON};

    @Override
    protected void tearDown() throws Exception {
        for (File file : myFiles2Delete) {
            file.delete();
        }
        super.tearDown();
    }

    public void testSimple() throws Exception {
        doTest("simple",
                //Test1
                "1:NONE\n" +
                "3:FULL\n" +
                "4:FULL\n" +

                //Common
                "1:FULL\n" +
                "4:FULL\n" +
                "7:NONE\n",

                //Test2
                "1:NONE\n" +
                "3:FULL\n" +
                "4:FULL\n" +

                //Common
                "1:FULL\n" +
                "4:NONE\n" +
                "7:FULL\n",

                //Test1
                "1:NONE\n" +
                "3:FULL\n" +
                "4:FULL\n" +

                //Test2
                "1:NONE\n" +
                "3:FULL\n" +
                "4:FULL\n" +

                //Common
                "1:FULL\n" +
                "4:FULL\n" +
                "7:FULL\n");
    }

    public void testJumps() throws Exception {
        doTest("jumps",
                //Test1
                "1:NONE\n" +
                "3:FULL\n" +
                "4:FULL\n" +
                 //Common
                "1:FULL\n" +
                "3:PARTIAL\n" +
                "4:NONE\n" +
                "6:FULL\n",

                //Test2
                "1:NONE\n" +
                "3:FULL\n" +
                "4:FULL\n" +
                //Common
                "1:FULL\n" +
                "3:PARTIAL\n" +
                "4:FULL\n" +
                "6:NONE\n",

                //Test1
                "1:NONE\n" +
                "3:FULL\n" +
                "4:FULL\n" +
                 //Test2
                "1:NONE\n" +
                "3:FULL\n" +
                "4:FULL\n" +
                 //Common
                "1:FULL\n" +
                "3:PARTIAL\n" +
                "4:FULL\n" +
                "6:FULL\n");
    }

    private ProjectData prepareData(String number, String testName) throws Exception {
        final String testDataPath = getTestDataPath(testName);
        String className = "Test" + number;
        File dataFile = new File(testDataPath + File.separator + className + ".ic");
        myFiles2Delete.add(dataFile);


        Main.compile(new String[]{testDataPath + File.separator + className + ".java",
                testDataPath + File.separator + COMMON + ".java",});

        myFiles2Delete.add(new File(testDataPath + File.separator + className + ".class"));
        myFiles2Delete.add(new File(testDataPath + File.separator + COMMON + "        .class"));

        final String exePath = System.getenv("JAVA_HOME") + File.separator + "bin" + File.separator + "java";
        final String coverageAgentPath = new File("").getAbsolutePath() + File.separator + "dist" + File.separator + "coverage-agent.jar";
        final Process process = Runtime.getRuntime().exec(new String[]{
                exePath,
                "-javaagent:" + coverageAgentPath + "=\"" + dataFile.getPath() + "\" false false false false .*",
                "-classpath", testDataPath,
                className
        });
        process.waitFor();
        process.destroy();

        while (!dataFile.exists()) {
            Thread.sleep(1000);
        }
        final ProjectData projectInfo = ProjectDataLoader.load(dataFile);
        assertNotNull(projectInfo);
        return projectInfo;
    }

    private void doTest(String testName, String expected1, String expected2, String expectedMerged) throws Exception {

        final ProjectData projectData1 = prepareData("1", testName);
        checkData(expected1, FRST_CLASS_NAMES, projectData1);

        final ProjectData projectData2 = prepareData("2", testName);
        checkData(expected2, SCD_CLASS_NAMES, projectData2);

        final ProjectData mergedData = new ProjectData();
        mergedData.merge(projectData1);
        mergedData.merge(projectData2);
        checkData(expectedMerged, ALL_CLASS_NAMES, mergedData);

        //check that data was not spoiled
        checkData(expected1, FRST_CLASS_NAMES, projectData1);
        checkData(expected2, SCD_CLASS_NAMES, projectData2);
    }

    private void checkData(String expected, String[] classNames, ProjectData projectData) {
        final StringBuffer buf1 = new StringBuffer();
        for (int i = 0, classNamesLength = classNames.length; i < classNamesLength; i++) {
            String className = classNames[i];
            if (className == null) {
                assertNull(projectData.getClassData(ALL_CLASS_NAMES[i]));
                continue;
            }
            final ClassData classInfo = projectData.getClassData(className);
            assertNotNull(classInfo);
            printLines(classInfo, buf1);
        }
        assertEquals(expected, buf1.toString());
    }

    private static void printLines(ClassData classInfo, final StringBuffer buf) {

        final Object[] objects = classInfo.getLines();
        final ArrayList<LineData> lines = new ArrayList<LineData>();
        for (Object object : objects) {
            if (object != null) {
                lines.add((LineData) object);
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
    }

    private static String getTestDataPath(String className) {
        return new File("").getAbsolutePath() + File.separator + "tests" + File.separator + "testData" + File.separator + "coverage" + File.separator + "merge" + File.separator + className;
    }

}
