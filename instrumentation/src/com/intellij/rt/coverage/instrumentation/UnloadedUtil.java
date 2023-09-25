/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.FileMapData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.data.instructions.InstructionsUtil;
import com.intellij.rt.coverage.instrumentation.dataAccess.EmptyCoverageDataAccess;
import com.intellij.rt.coverage.util.ErrorReporter;
import com.intellij.rt.coverage.util.classFinder.ClassEntry;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.util.Map;

/**
 * Some classes may be untouched during application execution, so these classes
 * have not been seen by coverage engine. In order to consider such classes in overall statistics,
 * coverage engine can analyse bytecode of these classes on disk.
 */
public class UnloadedUtil {
  public static final MethodVisitor EMPTY_METHOD_VISITOR = new MethodVisitor(Opcodes.API_VERSION) {
  };
  public static final ClassVisitor EMPTY_CLASS_VISITOR = new ClassVisitor(Opcodes.API_VERSION) {
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
      return EMPTY_METHOD_VISITOR;
    }
  };

  public static void appendUnloaded(final ProjectData projectData, final ClassFinder classFinder,
                                    final boolean calculateSource, final boolean branchCoverage) {
    classFinder.iterateMatchedClasses(new ClassEntry.Consumer() {
      public void consume(ClassEntry classEntry) {
        final ClassData cd = projectData.getClassData(classEntry.getClassName());
        if (cd != null && cd.getLines() != null && cd.isFullyAnalysed()) return;
        try {
          final InputStream is = classEntry.getClassInputStream();
          if (is == null) return;
          appendUnloadedClass(projectData, classEntry.getClassName(), new ClassReader(is), branchCoverage, calculateSource, false);
        } catch (Throwable e) {
          ErrorReporter.info("Failed to process unloaded class: " + classEntry.getClassName() + ", error: " + e.getMessage(), e);
        }
      }
    });
  }

  @SuppressWarnings("unused") // used in IntelliJ
  public static void appendUnloadedClass(ProjectData projectData, String className, ClassReader reader, boolean branchCoverage, boolean calculateSource) {
    appendUnloadedClass(projectData, className, reader, branchCoverage, calculateSource, true);
  }

  private static void appendUnloadedClass(ProjectData projectData, String className, ClassReader reader, boolean branchCoverage, boolean calculateSource, boolean checkLineMappings) {
    final ClassVisitor visitor = InstrumentationStrategy.createInstrumenter(
        projectData, className, reader, EMPTY_CLASS_VISITOR,
        null, branchCoverage, calculateSource, EmptyCoverageDataAccess.INSTANCE);
    if (visitor == null) return;
    reader.accept(visitor, ClassReader.SKIP_FRAMES);
    final ClassData classData = projectData.getClassData(className);
    if (classData == null || classData.getLines() == null) return;
    classData.dropIgnoredLines();
    final LineData[] lines = (LineData[]) classData.getLines();
    for (LineData line : lines) {
      if (line == null) continue;
      classData.registerMethodSignature(line);
    }
    if (!checkLineMappings) return;
    final Map<String, FileMapData[]> linesMap = projectData.getLinesMap();
    if (linesMap == null) return;
    final FileMapData[] mappings = linesMap.remove(className);
    if (mappings == null) return;
    classData.dropMappedLines(mappings);
    InstructionsUtil.dropMappedLines(projectData, classData.getName(), mappings);
  }
}
