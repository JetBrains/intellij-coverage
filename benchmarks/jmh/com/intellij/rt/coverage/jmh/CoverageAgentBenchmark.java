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

package com.intellij.rt.coverage.jmh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.util.NullOutputStream;

import java.io.PrintStream;

@SuppressWarnings({"unused", "used in jmh"})
public class CoverageAgentBenchmark {
  @Benchmark
  public void jodaTimeTestCoverage() throws Exception {
    PrintStream original = System.out;
    try {
      System.setOut(new PrintStream(new NullOutputStream()));
      JodaTimeTests.testCoverage();
    } finally {
      System.setOut(original);
    }
  }
}
