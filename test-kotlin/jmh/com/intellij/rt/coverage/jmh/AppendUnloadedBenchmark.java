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

import com.intellij.rt.coverage.data.*;
import com.intellij.rt.coverage.instrumentation.SourceLineCounter;
import com.intellij.rt.coverage.util.ErrorReporter;
import com.intellij.rt.coverage.util.LinesUtil;
import com.intellij.rt.coverage.util.StringsPool;
import com.intellij.rt.coverage.util.classFinder.ClassEntry;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.gnu.trove.TIntObjectProcedure;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
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
  final boolean isSampling = false;
  final boolean calculateSource = true;

  @Benchmark
  public int unloaded() throws Exception {
    final ProjectData projectData = ProjectData.createProjectData(new File("test.ic"), null, false, isSampling, Collections.<Pattern>emptyList(), Collections.<Pattern>emptyList(), null);
    createClassFinder().iterateMatchedClasses(new ClassEntry.Consumer() {
      @Override
      public void consume(ClassEntry classEntry) {
        processClassEntry(projectData, classEntry);
      }
    });
    System.out.println(projectData.getClassesNumber());
    return projectData.getClassesNumber();
  }

  private void processClassEntry(ProjectData projectData, ClassEntry classEntry) {
    ClassData cd = projectData.getClassData(StringsPool.getFromPool(classEntry.getClassName()));
    if (cd != null && cd.getLines() != null) return;
    try {
      final InputStream classInputStream = classEntry.getClassInputStream();
      if (classInputStream == null) return;
      ClassReader reader = new ClassReader(classInputStream);
      classInputStream.close();
      if (calculateSource) {
        cd = projectData.getOrCreateClassData(StringsPool.getFromPool(classEntry.getClassName()));
      }
      SourceLineCounter slc = new SourceLineCounter(cd, calculateSource ? projectData : null, !isSampling);
      reader.accept(slc, 0);
      if (slc.isEnum() || slc.getNSourceLines() > 0) { // ignore classes without executable code
        final TIntObjectHashMap<LineData> lines = new TIntObjectHashMap<LineData>(4, 0.99f);
        final int[] maxLine = new int[]{1};
        final ClassData classData = projectData.getOrCreateClassData(StringsPool.getFromPool(classEntry.getClassName()));
        slc.getSourceLines().forEachEntry(new TIntObjectProcedure<String>() {
          public boolean execute(int line, String methodSig) {
            final LineData ld = new LineData(line, StringsPool.getFromPool(methodSig));
            lines.put(line, ld);
            if (line > maxLine[0]) maxLine[0] = line;
            classData.registerMethodSignature(ld);
            ld.setStatus(LineCoverage.NONE);
            return true;
          }
        });
        final TIntObjectHashMap<JumpsAndSwitches> jumpsPerLine = slc.getJumpsPerLine();
        if (jumpsPerLine != null) {
          jumpsPerLine.forEachEntry(new TIntObjectProcedure<JumpsAndSwitches>() {
            public boolean execute(int line, JumpsAndSwitches jumpData) {
              final LineData lineData = lines.get(line);
              if (lineData != null) {
                lineData.setJumpsAndSwitches(jumpData);
                lineData.fillArrays();
              }
              return true;
            }
          });
        }
        classData.setLines(LinesUtil.calcLineArray(maxLine[0], lines));
      }
    } catch (Throwable e) {
      ErrorReporter.reportError("Failed to process unloaded class: " + classEntry.getClassName() + ", error: " + e.getMessage(), e);
    }
  }

  private ClassFinder createClassFinder() {
    final String absolutePath = "<Directory with output roots>";
    final Pattern includePattern = Pattern.compile("com\\..*");
    final ClassFinder finder = new ClassFinder(Collections.singletonList(includePattern), Collections.<Pattern>emptyList());
    final File dir = new File(absolutePath);
    final File[] files = dir.listFiles();
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
