/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

import com.intellij.rt.coverage.data.ProjectData;

import java.io.File;
import java.io.IOException;

public class CoverageRunner {
  private static final String[] EMPTY = new String[0];

  public static ProjectData runCoverage(String testDataPath, File coverageDataFile, final String patterns,
                                        String classToRun, final boolean branchCoverage) throws IOException, InterruptedException {
    return runCoverage(testDataPath, coverageDataFile, patterns, classToRun, branchCoverage, EMPTY, false, false);
  }

  public static ProjectData runCoverage(String testDataPath, File coverageDataFile, final String patterns,
                                        String classToRun, final boolean branchCoverage, String[] extraArgs, boolean calcUnloaded, boolean testTracking) throws IOException, InterruptedException {
    String coverageAgentPath = ResourceUtil.getAgentPath("intellij-coverage-agent");

    String[] commandLine = {
//        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5007",
        "-javaagent:" + coverageAgentPath + "=\"" + coverageDataFile.getPath() + "\" "
            + testTracking + " " + calcUnloaded + " false " + !branchCoverage + " " + patterns,
        "-Didea.coverage.test.mode=true",
        "-classpath", testDataPath, classToRun};
    if (extraArgs.length > 0) {
      String[] args = new String[extraArgs.length + commandLine.length];
      System.arraycopy(extraArgs, 0, args, 0, extraArgs.length);
      System.arraycopy(commandLine, 0, args, extraArgs.length, commandLine.length);
      commandLine = args;
    }
    ProcessUtil.execJavaProcess(commandLine);

    FileUtil.waitUntilFileCreated(coverageDataFile);
    final ProjectData projectInfo = ProjectDataLoader.loadLocked(coverageDataFile);
    assert projectInfo != null;
    return projectInfo;
  }
}
