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

package com.intellij.rt.coverage.util;

import org.junit.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessUtil {
  public static void execJavaProcess(String[] cmd) throws InterruptedException, IOException {
    String javaHome = System.getenv("JAVA_HOME");
    if (javaHome == null) {
      Assert.fail("JAVA_HOME environment variable needs to be set");
    }
    final String exePath = javaHome + File.separator + "bin" + File.separator + "java";

    String[] fullCmd = new String[cmd.length + 1];
    fullCmd[0] = exePath;
    System.arraycopy(cmd, 0, fullCmd, 1, cmd.length);
    final Process process = Runtime.getRuntime().exec(fullCmd);
    process.waitFor();

    if (process.exitValue() != 0) {
      printStdout(process);
      process.destroy();
      throw new RuntimeException("Exit code != 0");
    }

    process.destroy();
  }

  private static void printStdout(Process process) throws IOException {
    BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
    BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
    String str;

    while ((str = output.readLine()) != null) {
      System.out.println(str);
    }

    while ((str = error.readLine()) != null) {
      System.out.println(str);
    }
  }
}
