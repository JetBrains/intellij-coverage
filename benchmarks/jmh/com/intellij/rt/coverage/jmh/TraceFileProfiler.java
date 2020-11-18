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

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.results.AggregationPolicy;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.ScalarResult;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

@SuppressWarnings({"unused", "in jmh benchmarks"})
public class TraceFileProfiler implements InternalProfiler {
  public String getDescription() {
    return null;
  }

  public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
  }

  public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams,
                                                     IterationParams iterationParams,
                                                     IterationResult result) {
    File file = traceFile();
    return Collections.singletonList(new ScalarResult(
        "trace.file.size", file.exists() ? file.length() / 1024.0 / 1024.0 : 0,
        "MB", AggregationPolicy.AVG)
    );
  }

  private static File traceFile() {
    return getCanonicalFile(new File(System.getProperty("org.jetbrains.instrumentation.trace.file", "td.ijtc")));
  }

  private static File getCanonicalFile(File file) {
    try {
      return file.getCanonicalFile();
    } catch (IOException e) {
      return file.getAbsoluteFile();
    }
  }
}
