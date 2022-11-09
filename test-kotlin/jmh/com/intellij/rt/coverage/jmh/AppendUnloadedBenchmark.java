/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.SaveHook;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;
import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@BenchmarkMode(Mode.SingleShotTime)
@Measurement(iterations = 10)
@Warmup(iterations = 10)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(1)
public class AppendUnloadedBenchmark {
  @Param({"false", "true"})
  boolean isSampling = true;
  final boolean calculateSource = true;

  @Benchmark
  public int instrumentation() throws Exception {
    final ClassFinder classFinder = createClassFinder();
    final File dataFile = new File("test.ic");
    final ProjectData projectData = ProjectData.createProjectData(dataFile, null, false, isSampling, Collections.<Pattern>emptyList(), Collections.<Pattern>emptyList(), null);
    SaveHook.appendUnloadedFullAnalysis(projectData, classFinder, calculateSource, isSampling, false);
    System.out.println(projectData.getClassesNumber());
    assert dataFile.delete();
    return projectData.getClassesNumber();
  }

  @Benchmark
  public int unloaded() throws Exception {
    final ClassFinder classFinder = createClassFinder();
    final File dataFile = new File("test.ic");
    final ProjectData projectData = ProjectData.createProjectData(dataFile, null, false, isSampling, Collections.<Pattern>emptyList(), Collections.<Pattern>emptyList(), null);
    SaveHook.appendUnloaded(projectData, classFinder, calculateSource, isSampling);
    System.out.println(projectData.getClassesNumber());
    assert dataFile.delete();
    return projectData.getClassesNumber();
  }

  private ClassFinder createClassFinder() {
    final String absolutePath = "<Directory with output roots>";
    final File dir = new File(absolutePath);
    final File[] files = dir.listFiles();
    if (!dir.exists() || files == null) {
      throw new IllegalArgumentException("Please set directory with output roots in com.intellij.rt.coverage.jmh.AppendUnloadedBenchmark.createClassFinder");
    }
    final Pattern includePattern = Pattern.compile("com\\..*");
    final ClassFinder finder = new ClassFinder(Collections.singletonList(includePattern), Collections.<Pattern>emptyList());
    for (File child : files) {
      try {
        final URL url = child.toURI().toURL();
        finder.addClassLoader(new URLClassLoader(new URL[]{url}));
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }

    return finder;
  }

  public static void main(String[] args) throws Exception {
    AppendUnloadedBenchmark b = new AppendUnloadedBenchmark();
    b.unloaded();
  }
}
