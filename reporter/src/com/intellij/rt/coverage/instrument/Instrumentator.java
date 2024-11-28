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

package com.intellij.rt.coverage.instrument;

import com.intellij.rt.coverage.instrumentation.CoverageTransformer;
import com.intellij.rt.coverage.instrumentation.InstrumentationOptions;
import com.intellij.rt.coverage.report.api.Filters;
import com.intellij.rt.coverage.util.ClassNameUtil;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * Perform offline instrumentation of class files in specified output roots.
 * If a class is not included into coverage instrumentation via filters,
 * its class file is copied to new output directory without modifications.
 */
public class Instrumentator {
  private final List<File> myRoots;
  private final List<File> myOutputRoots;
  private final Filters myFilters;
  /**
   * This loader is provided to the instrumenter for correct frames computation in ClassWriterImpl.
   */
  private final ClassLoader myLoader;

  public Instrumentator(List<File> roots, List<File> outputRoots, Filters filters) {
    myRoots = roots;
    myOutputRoots = outputRoots;
    myFilters = filters;
    myLoader = createClassLoader(roots);
  }

  private ClassLoader createClassLoader(List<File> roots) {
    URL[] urls = new URL[roots.size()];
    for (int i = 0; i < roots.size(); i++) {
      try {
        urls[i] = myRoots.get(i).toURI().toURL();
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }
    return new URLClassLoader(urls);
  }

  public void instrument(final boolean countHits) {
    for (int i = 0; i < myRoots.size(); i++) {
      final File root = myRoots.get(i);
      final File outputRoot = myOutputRoots.get(i);
      new InstrumentationVisitor(root, outputRoot, countHits).visitFiles();
    }
  }

  public static byte[] instrument(final byte[] bytes, boolean countHits) {
    InstrumentationOptions options = new InstrumentationOptions.Builder().setIsCalculateHits(countHits).build();
    CoverageTransformer transformer = new OfflineCoverageTransformer(options);
    String className = new ClassReader(bytes).getClassName();
    // This loader is not user actually, just need some not null loader
    ClassLoader loader = ClassLoader.getSystemClassLoader();
    return transformer.transform(loader, className, bytes, null);
  }

  private class InstrumentationVisitor extends DirectoryVisitor {
    private final File myOutput;
    private final CoverageTransformer myTransformer;

    private InstrumentationVisitor(File root, File output, boolean countHits) {
      super(root);
      myOutput = output;
      InstrumentationOptions options = new InstrumentationOptions.Builder()
          .setIsCalculateHits(countHits)
          .setExcludeAnnotations(myFilters.excludeAnnotations)
          .build();
      myTransformer = new OfflineCoverageTransformer(options);
    }

    @Override
    protected void visitFile(String packageName, File file) {
      try {
        byte[] bytes = IOUtil.readBytes(file);

        if (file.getName().endsWith(ClassNameUtil.CLASS_FILE_SUFFIX)) {
          final String classSimpleName = ClassNameUtil.removeClassSuffix(file.getName());
          final String className = packageName.isEmpty()
              ? classSimpleName
              : ClassNameUtil.convertToInternalName(packageName) + "/" + classSimpleName;
          final byte[] transformed = myTransformer.transform(myLoader, className, null, null, bytes);
          if (transformed != null) {
            bytes = transformed;
          }
        }

        final File directory = new File(myOutput, packageName.replace(".", File.separator));
        if (!directory.exists() && !directory.mkdirs()) {
          throw new RuntimeException("Failed to create directory at " + directory.getAbsolutePath());
        }

        final File newFile = new File(directory, file.getName());
        IOUtil.writeBytes(newFile, bytes);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
