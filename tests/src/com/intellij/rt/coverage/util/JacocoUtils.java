/*
 * Copyright 2000-2024 JetBrains s.r.o.
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

import com.intellij.rt.coverage.data.*;
import com.intellij.rt.coverage.instrumentation.CoverageArgs;
import org.jacoco.core.analysis.*;
import org.jacoco.core.tools.ExecFileLoader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class JacocoUtils {
  static ProjectData loadExecData(File execFile, String classPath, CoverageArgs coverageArgs) throws IOException {
    List<File> outputRoots = Arrays.stream(classPath.split(":")).map(File::new).filter(File::isDirectory).collect(Collectors.toList());
    ProjectData projectData = new ProjectData();
    loadExecutionData(execFile, projectData, outputRoots, coverageArgs);
    return projectData;
  }

  private static void loadExecutionData(File sessionDataFile, ProjectData data, List<File> roots, CoverageArgs coverageArgs) throws IOException {
    ExecFileLoader loader = new ExecFileLoader();
    final CoverageBuilder coverageBuilder = new CoverageBuilder();
    loadReportToCoverageBuilder(coverageBuilder, sessionDataFile, loader, roots, coverageArgs);

    for (IClassCoverage classCoverage : coverageBuilder.getClasses()) {
      String className = internalNameToFqn(classCoverage.getName());
      final ClassData classData = data.getOrCreateClassData(className);
      final Collection<IMethodCoverage> methods = classCoverage.getMethods();
      LineData[] lines = new LineData[classCoverage.getLastLine() + 1];
      for (IMethodCoverage method : methods) {
        final String desc = method.getName() + method.getDesc();
        // Line numbers are 1-based here.
        final int firstLine = method.getFirstLine();
        final int lastLine = method.getLastLine();
        for (int i = firstLine; i <= lastLine; i++) {
          final ILine methodLine = method.getLine(i);
          final int methodLineStatus = methodLine.getStatus();
          if (methodLineStatus == ICounter.EMPTY) continue;
          final LineData lineData = new LineData(i, desc);
          switch (methodLineStatus) {
            case ICounter.FULLY_COVERED:
              lineData.setStatus(LineCoverage.FULL);
              break;
            case ICounter.PARTLY_COVERED:
              lineData.setStatus(LineCoverage.PARTIAL);
              break;
            default:
              lineData.setStatus(LineCoverage.NONE);
              break;
          }

          lineData.setHits(methodLineStatus == ICounter.FULLY_COVERED || methodLineStatus == ICounter.PARTLY_COVERED ? 1 : 0);
          ICounter branchCounter = methodLine.getBranchCounter();
          if (branchCounter.getTotalCount() > 0) {
            final int[] keys = new int[branchCounter.getTotalCount()];
            for (int key = 0; key < keys.length; key++) {
              keys[key] = key;
            }
            final SwitchData switchData = lineData.addSwitch(0, keys);
            final int[] hits = switchData.getHits();
            Arrays.fill(hits, 0, branchCounter.getCoveredCount(), 1);
            switchData.setKeysAndHits(keys, hits);
            switchData.setDefaultHits(1);
          }

          classData.registerMethodSignature(lineData);
          lineData.fillArrays();
          lines[i] = lineData;
        }
      }
      classData.setLines(lines);
    }
  }

  private static void loadReportToCoverageBuilder(CoverageBuilder coverageBuilder,
                                                  File sessionDataFile,
                                                  ExecFileLoader loader,
                                                  List<File> roots, CoverageArgs coverageArgs) throws IOException {
    loader.load(sessionDataFile);

    final Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), coverageBuilder);

    for (File root : roots) {
      try {
        Path rootPath = root.toPath();
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
          public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
            File file = path.toFile();
            String internalName = rootPath.relativize(path).toString().replace(".class", "");
            String fqn = internalNameToFqn(internalName);
            if (!shouldIncludeClass(coverageArgs, fqn)) return FileVisitResult.CONTINUE;
            try {
              analyzer.analyzeAll(file);
            } catch (Exception e) {
              e.printStackTrace();
            }
            return FileVisitResult.CONTINUE;
          }
        });
      } catch (NoSuchFileException ignore) {
      }
    }
  }

  public static @NotNull String internalNameToFqn(@NotNull String internalName) {
    return internalName.replace('\\', '.').replace('/', '.');
  }

  private static boolean shouldIncludeClass(CoverageArgs coverageArgs, String className) {
    if (ClassNameUtil.matchesPatterns(className, coverageArgs.excludePatterns)) return false;
    List<Pattern> includePatterns = coverageArgs.includePatterns;
    return includePatterns == null || includePatterns.isEmpty() || ClassNameUtil.matchesPatterns(className, includePatterns);
  }
}
