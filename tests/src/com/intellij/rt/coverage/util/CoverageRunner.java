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
import com.intellij.rt.coverage.instrumentation.CoverageArgs;
import org.jacoco.agent.AgentJar;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class CoverageRunner {
  private static final String[] EMPTY = new String[0];

  public static ProjectData runCoverage(String testDataPath, File coverageDataFile, final String patterns,
                                        String classToRun, final boolean branchCoverage) throws IOException, InterruptedException {
    return runCoverage(testDataPath, coverageDataFile, patterns, classToRun, branchCoverage, EMPTY, false, false);
  }

  public static ProjectData runCoverage(String testDataPath, File coverageDataFile, final String patterns,
                                        String classToRun, final boolean branchCoverage, String[] extraArgs,
                                        boolean calcUnloaded, boolean testTracking) throws IOException, InterruptedException {
    String coverageAgentPath = ResourceUtil.getAgentPath("intellij-coverage-agent");
    return runCoverage(coverageAgentPath, testDataPath, coverageDataFile, patterns, classToRun, branchCoverage, extraArgs, calcUnloaded, testTracking);
  }

  public static ProjectData runCoverage(String coverageAgentPath,
                                        String testDataPath, File coverageDataFile, final String patterns,
                                        String classToRun, final boolean branchCoverage, String[] extraArgs,
                                        boolean calcUnloaded, boolean testTracking) throws IOException, InterruptedException {
    String jacocoAgentPath = AgentJar.extractToTempLocation().getPath();
    CoverageArgs coverageArgs = new CoverageArgs();
    coverageArgs.readPatterns(Arrays.stream(patterns.split(" ")).filter(s -> !s.isEmpty()).toArray(String[]::new), 0);
    StringBuilder jacocoArgs = new StringBuilder();
    jacocoArgs.append("-javaagent:").append(jacocoAgentPath).append("=")
        .append("destfile=").append(coverageDataFile.getPath())
        .append(",append=false");
    if (!coverageArgs.includePatterns.isEmpty()) {
      jacocoArgs.append(",includes=")
          .append(String.join(":", extractPatterns(coverageArgs.includePatterns)));
    }
    if (!coverageArgs.excludePatterns.isEmpty()) {
      jacocoArgs.append(",excludes=")
          .append(String.join(":", extractPatterns(coverageArgs.excludePatterns)));
    }
    String[] commandLine = {
//        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5007",
        jacocoArgs.toString(),
        "-classpath", testDataPath, classToRun};
    if (extraArgs.length > 0) {
      String[] args = new String[extraArgs.length + commandLine.length];
      System.arraycopy(extraArgs, 0, args, 0, extraArgs.length);
      System.arraycopy(commandLine, 0, args, extraArgs.length, commandLine.length);
      commandLine = args;
    }
    ProcessUtil.execJavaProcess(commandLine);

    FileUtil.waitUntilFileCreated(coverageDataFile);
    final ProjectData projectInfo = JacocoUtils.loadExecData(coverageDataFile, testDataPath, coverageArgs);
    assert projectInfo != null;
    return projectInfo;
  }

  private static String @NotNull [] extractPatterns(List<Pattern> includePatterns) {
    return includePatterns.stream().map(Pattern::pattern).map(p ->
        p.replace("\\.", ".").replace(".*", "*")
    ).toArray(String[]::new);
  }
}
