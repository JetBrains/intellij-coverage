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
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.data.ProjectContext;
import com.intellij.rt.coverage.instrumentation.dataAccess.EmptyCoverageDataAccess;
import com.intellij.rt.coverage.util.ErrorReporter;
import com.intellij.rt.coverage.util.classFinder.ClassEntry;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

import java.io.InputStream;

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

  @SuppressWarnings("unused") // used in IntelliJ
  public static void appendUnloaded(ProjectData projectData, ClassFinder classFinder, boolean branchCoverage) {
    InstrumentationOptions options = new InstrumentationOptions.Builder().setBranchCoverage(branchCoverage).build();
    appendUnloaded(projectData, new ProjectContext(options, classFinder), true);
  }

  public static void appendUnloaded(ProjectData projectData, ProjectContext context) {
    appendUnloaded(projectData, context, false);
  }

  private static void appendUnloaded(final ProjectData projectData, final ProjectContext context, final boolean finalizeCoverage) {
    context.getClassFinder().iterateMatchedClasses(new ClassEntry.Consumer() {
      public void consume(ClassEntry classEntry) {
        final ClassData cd = projectData.getClassData(classEntry.getClassName());
        if (cd != null && cd.getLines() != null && cd.isFullyAnalysed()) return;
        try {
          final InputStream is = classEntry.getClassInputStream();
          if (is == null) return;
          appendUnloadedClass(projectData, classEntry.getClassName(), new ClassReader(is), context, finalizeCoverage);
        } catch (Throwable e) {
          ErrorReporter.info("Failed to process unloaded class: " + classEntry.getClassName() + ", error: " + e.getMessage(), e);
        }
      }
    });
  }

  @SuppressWarnings("unused") // used in IntelliJ
  public static void appendUnloadedClass(ProjectData projectData, String className, ClassReader reader, boolean branchCoverage) {
    InstrumentationOptions options = new InstrumentationOptions.Builder().setBranchCoverage(branchCoverage).build();
    appendUnloadedClass(projectData, className, reader, new ProjectContext(options), true);
  }

  private static void appendUnloadedClass(ProjectData projectData, String className, ClassReader reader, ProjectContext context, boolean finalizeCoverage) {
    ClassVisitor cv = InstrumentationStrategy.createInstrumenter(projectData, className, reader,
        EMPTY_CLASS_VISITOR, context, EmptyCoverageDataAccess.INSTANCE);
    if (cv == null) return;
    reader.accept(cv, ClassReader.SKIP_FRAMES);
    final ClassData classData = projectData.getClassData(className);
    if (classData == null || classData.getLines() == null) return;
    final LineData[] lines = (LineData[]) classData.getLines();
    for (LineData line : lines) {
      if (line == null) continue;
      classData.registerMethodSignature(line);
    }
    if (finalizeCoverage) {
      context.dropLineMappings(projectData, classData);
    }
  }
}
