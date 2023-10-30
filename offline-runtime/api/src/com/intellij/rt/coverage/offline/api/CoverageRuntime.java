/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

package com.intellij.rt.coverage.offline.api;

import com.intellij.rt.coverage.offline.RawHitsReport;
import com.intellij.rt.coverage.offline.RawProjectData;
import com.intellij.rt.coverage.offline.RawProjectInit;
import com.intellij.rt.coverage.util.ClassNameUtil;
import com.intellij.rt.coverage.util.ErrorReporter;
import com.intellij.rt.coverage.util.classFinder.ClassEntry;
import com.intellij.rt.coverage.util.classFinder.ClassFilter;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;
import com.intellij.rt.coverage.util.classFinder.OutputClassFinder;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;

import java.io.*;
import java.util.List;

public class CoverageRuntime {
  public static List<ClassCoverage> collectInRoots(List<File> roots) {
    RawProjectData projectData = getProjectData();
    ClassFinder classFinder = new OutputClassFinder(null, roots);
    return CoverageCollector.collect(projectData, classFinder);
  }

  public static List<ClassCoverage> collectClassfileData(final List<byte[]> classFiles) {
    RawProjectData projectData = getProjectData();
    ClassFinder classFinder = new ClassListFinder(null, classFiles);
    return CoverageCollector.collect(projectData, classFinder);
  }

  public static void dumpIcReport(DataOutput output, File errorFile) throws IOException {
    ErrorReporter.setPath(errorFile.getPath());
    RawHitsReport.dump(output, getProjectData());
  }

  private static RawProjectData getProjectData() {
    return RawProjectInit.getProjectData();
  }

  private static class ClassFileEntry extends ClassEntry {
    private final byte[] classFile;

    public ClassFileEntry(String className, byte[] classFile) {
      super(className);
      this.classFile = classFile;
    }

    @Override
    public InputStream getClassInputStream() {
      return new ByteArrayInputStream(classFile);
    }
  }

  private static class ClassListFinder extends ClassFinder {
    private final List<byte[]> classFiles;

    public ClassListFinder(ClassFilter filter, List<byte[]> classFiles) {
      super(filter);
      this.classFiles = classFiles;
    }

    @Override
    public void iterateMatchedClasses(ClassEntry.Consumer consumer) {
      for (final byte[] classFile : classFiles) {
        ClassReader reader = new ClassReader(classFile);
        String className = ClassNameUtil.convertToFQName(reader.getClassName());
        consumer.consume(new ClassFileEntry(className, classFile));
      }
    }
  }
}
