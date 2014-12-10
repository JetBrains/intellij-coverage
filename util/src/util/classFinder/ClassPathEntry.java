/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Pavel.Sher
 */
public class ClassPathEntry {
  private ClassLoader myClassLoader;
  private String myClassPathEntry;

  public ClassPathEntry(final String classPathEntry, final ClassLoader classLoader) {
    myClassPathEntry = classPathEntry;
    myClassLoader = classLoader;
  }

  public Collection getClassesIterator(List includePatterns, List excludePatterns) throws IOException {
    ClassPathEntryProcessor processor = createEntryProcessor(myClassPathEntry);
    if (processor == null) {
//      System.err.println("Do not know how to process class path entry: " + myClassPathEntry);
      return Collections.emptyList();
    }                          
    processor.setFilter(includePatterns, excludePatterns);
    processor.setClassLoader(myClassLoader);
    return processor.findClasses(myClassPathEntry);
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
    private List myIncludePatterns;
    private List myExcludePatterns;
    private ClassLoader myClassLoader;

    public void setFilter(final List includePatterns, final List excludePatterns) {
      myIncludePatterns = includePatterns;
      myExcludePatterns = excludePatterns;
    }

    public void setClassLoader(final ClassLoader classLoader) {
      myClassLoader = classLoader;
    }

    protected abstract Collection extractClassNames(String classPathEntry) throws IOException;

    public Collection findClasses(final String classPathEntry) throws IOException {
      Set includedClasses = new HashSet();
      for (Iterator iterator = extractClassNames(classPathEntry).iterator(); iterator.hasNext();) {
        final String className = (String) iterator.next();
        if (shouldInclude(className)) {
          includedClasses.add(new ClassEntry(className, myClassLoader));
        }
      }
      return includedClasses;
    }

    protected boolean shouldInclude(final String className) {
      // matching outer or inner class name depending on pattern
      if (ClassNameUtil.shouldExclude(className, myExcludePatterns)) return false;

      String outerClassName = ClassNameUtil.getOuterClassName(className);
      for (Iterator it = myIncludePatterns.iterator(); it.hasNext();) {
        Pattern e = (Pattern)it.next();
        if (e.matcher(outerClassName).matches()) return true;
      }
      return myIncludePatterns.isEmpty();
    }
  }

  private interface ClassPathEntryProcessor {
    void setFilter(final List includePatterns, final List excludePatterns);
    void setClassLoader(final ClassLoader classLoader);

    Collection findClasses(final String classPathEntry) throws IOException;
  }

  private static final String CLASS_FILE_SUFFIX = ".class";

  private static class DirectoryEntryProcessor extends AbstractClassPathEntryProcessor {

    protected Collection extractClassNames(final String classPathEntry) {
      File dir = new File(classPathEntry);
      List result = new ArrayList(100);
      String curPath = "";
      collectClasses(curPath, dir, result);
      return result;
    }

    private static void collectClasses(final String curPath, final File parent, final List result) {
      File[] files = parent.listFiles();
      if (files != null) {
        String prefix = curPath.length() == 0 ? "" : curPath + ".";
        for (int i = 0; i < files.length; i++) {
          File f = files[i];
          final String name = f.getName();
          if (name.endsWith(CLASS_FILE_SUFFIX)) {
            result.add(prefix + removeClassSuffix(name));
          }
          else if (f.isDirectory()) {
            collectClasses(prefix + name, f, result);
          }
        }
      }
    }
  }

  private static String removeClassSuffix(final String name) {
    return name.substring(0, name.length() - CLASS_FILE_SUFFIX.length());
  }

  private static class ZipEntryProcessor extends AbstractClassPathEntryProcessor {
    public Collection extractClassNames(final String classPathEntry) throws IOException {
      List result = new ArrayList(100);
      ZipFile zipFile = new ZipFile(new File(classPathEntry));
      try {
        Enumeration zenum = zipFile.entries();
        while (zenum.hasMoreElements()) {
          ZipEntry ze = (ZipEntry)zenum.nextElement();
          if (!ze.isDirectory() && ze.getName().endsWith(CLASS_FILE_SUFFIX)) {
            result.add(removeClassSuffix(ze.getName()).replace('/', '.').replace('\\', '.'));
          }
        }
      } finally {
        zipFile.close();
      }
      return result;
    }
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ClassPathEntry that = (ClassPathEntry) o;

    if (myClassLoader != null ? !myClassLoader.equals(that.myClassLoader) : that.myClassLoader != null) return false;
    if (!myClassPathEntry.equals(that.myClassPathEntry)) return false;

    return true;
  }

  public int hashCode() {
    int result = myClassLoader != null ? myClassLoader.hashCode() : 0;
    result = 31 * result + myClassPathEntry.hashCode();
    return result;
  }
}
