/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package com.intellij.rt.coverage;

import com.intellij.rt.coverage.data.ProjectData;
import com.sun.tools.javac.Main;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;


public class CoverageExclusionInclusionTest extends TestCase {
  private File myDataFile;
  private File[] myClassFiles;

  @Override
  protected void tearDown() throws Exception {
    if (myDataFile != null) myDataFile.delete();
    if (myClassFiles != null) {
      for (File myClassFile : myClassFiles) {
        myClassFile.delete();
      }
    }
    super.tearDown();
  }

  public void testIncludeAllExplicitly() throws Exception {
    doTest(".*", "Test", "Foo", "Bar");
  }

  public void testIncludeAllImplicitly() throws Exception {
    doTest("", "Test", "Foo", "Bar");
  }

  public void testIncludeOnlyFoo() throws Exception {
    doTest("F.*o", "Foo");
  }

  public void testIncludeFooAndBar() throws Exception {
    doTest("F.*o Bar", "Foo", "Bar");
  }

  private void doTest(String inclusionFilter, String... expectedClasses) throws Exception {
    File testDataPath = getTestDataFile();
    myDataFile = new File(testDataPath, "Test.ic");

    if (Main.compile(new String[]{testDataPath.getAbsolutePath() + File.separator + "Test.java"}) != 0) {
      fail("Compilation failed");
    }

    myClassFiles = testDataPath.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.endsWith(".class");
      }
    });

    final ProjectData projectInfo = CoverageStatusTest.runCoverage(testDataPath.getAbsolutePath(), myDataFile, inclusionFilter, "Test", true);
    assertEquals(new HashSet(Arrays.asList(expectedClasses)), projectInfo.getClasses().keySet());
  }

  @NotNull
  private File getTestDataFile() {
    return new File("testData" + File.separator + "coverage" + File.separator + "exclusionInclusion");
  }
}
