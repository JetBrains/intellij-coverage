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

package com.intellij.rt.coverage.testDiscovery;

import com.intellij.rt.coverage.util.ProcessUtil;
import com.intellij.rt.coverage.util.ResourceUtil;
import com.intellij.rt.coverage.util.StringUtil;
import com.sun.tools.javac.Main;
import junit.framework.TestCase;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.runner.JUnitLauncher;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDiscoveryTestUtil {
  static void compileTestData(File sourceDir, File outputDir) {
    final File[] sources = sourceDir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.endsWith(".java");
      }
    });
    assertThat(sources).isNotNull().isNotEmpty();

    final ArrayList<String> args = new ArrayList<String>();
    args.add("-d");
    args.add(outputDir.getAbsolutePath());
    args.add("-cp");
    args.add(ResourceUtil.getResourceRoot(TestCase.class)); // junit
    for (File source : sources) {
      args.add(source.getAbsolutePath());
    }

    if (Main.compile(args.toArray(new String[0])) != 0) {
      Assert.fail("Compilation failed");
    }
  }

  static void runTestDiscovery(String testDataPath, String testClass, List<String> additionalJavaOptions) throws IOException, InterruptedException {
    String agentJar = ResourceUtil.getAgentPath("test-discovery-agent");

    final ArrayList<String> args = new ArrayList<String>();
    // args.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5007");
    args.add("-javaagent:" + agentJar);
    args.add("-classpath");
    args.add(StringUtil.join(File.pathSeparator, testDataPath,
        ResourceUtil.getResourceRoot(JUnitLauncher.class),
        ResourceUtil.getResourceRoot(TestCase.class),
        ResourceUtil.getResourceRoot(Matcher.class)));
    args.addAll(additionalJavaOptions);
    args.add("org.junit.runner.JUnitLauncher");
    args.add(testClass);

    ProcessUtil.execJavaProcess(args.toArray(new String[0]));
  }
}
