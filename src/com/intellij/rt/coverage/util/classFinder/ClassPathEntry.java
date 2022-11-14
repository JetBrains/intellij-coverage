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

package com.intellij.rt.coverage.util.classFinder;

import com.intellij.rt.coverage.util.ClassNameUtil;
import com.intellij.rt.coverage.util.CoverageIOUtil;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Pavel.Sher
 */
public class ClassPathEntry {
  private final String myClassPathEntry;

  // Used in IntelliJ
  @SuppressWarnings("WeakerAccess")
  public ClassPathEntry(final String classPathEntry) {
    myClassPathEntry = classPathEntry;
  }

  void iterateMatchedClasses(ClassFilter filter, ClassEntry.Consumer consumer) throws IOException {
    ClassPathEntryProcessor processor = createEntryProcessor(myClassPathEntry);
    if (processor == null) {
//      System.err.println("Do not know how to process class path entry: " + myClassPathEntry);
      return;
    }
    processor.setFilter(filter);
    processor.iterateMatchedClasses(myClassPathEntry, consumer);
  }

  private static ClassPathEntryProcessor createEntryProcessor(String entry) {
    File file = new File(entry);
    if (file.isDirectory()) {
      return myDirectoryProcessor;
    }
    if (file.isFile() && (file.getName().endsWith(".jar") || file.getName().endsWith(".zip"))) {
      return myZipProcessor;
    }
    return null;
  }

  private final static DirectoryEntryProcessor myDirectoryProcessor = new DirectoryEntryProcessor();
  private final static ZipEntryProcessor myZipProcessor = new ZipEntryProcessor();

  private static abstract class AbstractClassPathEntryProcessor implements ClassPathEntryProcessor {
    private ClassFilter myFilter;

    public void setFilter(ClassFilter filter) {
      myFilter = filter;
    }
    protected final boolean shouldInclude(final String className) {
      return myFilter.shouldInclude(className);
    }
  }

  private interface ClassPathEntryProcessor {
    void setFilter(ClassFilter filter);

    void iterateMatchedClasses(final String classPathEntry, ClassEntry.Consumer consumer) throws IOException;
  }

  private static class DirectoryEntryProcessor extends AbstractClassPathEntryProcessor {

    public void iterateMatchedClasses(final String classPathEntry, ClassEntry.Consumer consumer) throws IOException {
      File dir = new File(classPathEntry);
      final InputStream[] is = new InputStream[] {null};
      collectClasses("", dir, consumer, is);
    }

    private void collectClasses(final String curPath, final File parent, final ClassEntry.Consumer consumer, final InputStream[] is) throws IOException {
      File[] files = parent.listFiles();
      if (files != null) {
        String prefix = curPath.length() == 0 ? "" : curPath + ".";
        for (final File f : files) {
          final String name = f.getName();
          if (name.endsWith(ClassNameUtil.CLASS_FILE_SUFFIX)) {
            final String className = prefix + ClassNameUtil.removeClassSuffix(name);
            if (shouldInclude(className)) {
              is[0] = null;
              try {
                consumer.consume(new ClassEntry(className) {
                  @Override
                  public InputStream getClassInputStream() throws IOException {
                    is[0] = new FileInputStream(f);
                    return is[0];
                  }
                });
              } finally {
                CoverageIOUtil.close(is[0]);
              }
            }
          } else if (f.isDirectory()) {
            collectClasses(prefix + name, f, consumer, is);
          }
        }
      }
    }
  }

  private static class ZipEntryProcessor extends AbstractClassPathEntryProcessor {
    public void iterateMatchedClasses(final String classPathEntry, ClassEntry.Consumer consumer) throws IOException {
      final ZipFile zipFile = new ZipFile(new File(classPathEntry));
      try {
        final InputStream[] is = new InputStream[] {null};
        Enumeration<? extends ZipEntry> zenum = zipFile.entries();
        while (zenum.hasMoreElements()) {
          ZipEntry ze = zenum.nextElement();
          if (!ze.isDirectory() && ze.getName().endsWith(ClassNameUtil.CLASS_FILE_SUFFIX)) {
            final String className = ClassNameUtil.convertToFQName(ClassNameUtil.removeClassSuffix(ze.getName()));
            if (shouldInclude(className)) {
              is[0] = null;
              try {
                final ZipEntry zipEntry = ze;
                consumer.consume(new ClassEntry(className) {
                  public InputStream getClassInputStream() throws IOException {
                    is[0] = zipFile.getInputStream(zipEntry);
                    return is[0];
                  }
                });
              } finally {
                CoverageIOUtil.close(is[0]);
              }
            }
          }
        }
      } finally {
        zipFile.close();
      }
    }
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ClassPathEntry that = (ClassPathEntry) o;

    return myClassPathEntry.equals(that.myClassPathEntry);
  }

  public int hashCode() {
    return myClassPathEntry.hashCode();
  }
}
