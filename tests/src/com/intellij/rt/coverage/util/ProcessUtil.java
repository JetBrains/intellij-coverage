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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessUtil {
  public static void execProcess(String[] cmd) throws InterruptedException, IOException {
    final Process process = Runtime.getRuntime().exec(cmd);
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
