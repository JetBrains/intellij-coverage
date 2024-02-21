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

import com.intellij.rt.coverage.util.ErrorReporter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author pavel.sher
 */
public class ClassFinder {
  private final ClassFilter myFilter;
  private final Set<ClassLoader> myClassloaders;

  public ClassFinder(List<Pattern> includePatterns, List<Pattern> excludePatterns) {
    this(new ClassFilter.PatternFilter(includePatterns, excludePatterns));
  }

  public ClassFinder(ClassFilter filter) {
    myFilter = filter;
    myClassloaders = new HashSet<ClassLoader>();
  }

  public void addClassLoader(ClassLoader cl) {
    if (cl != null) {
      // workaround for TeamCity own tests
      if (cl.getClass().getName().equals("jetbrains.buildServer.agent.AgentClassLoader")) return;

      if (cl instanceof URLClassLoader) {
        myClassloaders.add(cl);
      }
      if (cl.getParent() != null) {
        addClassLoader(cl.getParent());
      }
    }
  }

  public void iterateMatchedClasses(ClassEntry.Consumer consumer) {
    for (ClassPathEntry entry : getClassPathEntries()) {
      try {
        entry.iterateMatchedClasses(myFilter, consumer);
      } catch (IOException e) {
        ErrorReporter.info("Error during iterating classes.", e);
      }
    }
  }

  protected Collection<ClassPathEntry> getClassPathEntries() {
    Set<ClassPathEntry> result = new HashSet<ClassPathEntry>();
    result.addAll(extractEntries(System.getProperty("java.class.path")));
    result.addAll(extractEntries(System.getProperty("sun.boot.class.path")));
    collectClassloaderEntries(result);
    return result;
  }

  private void collectClassloaderEntries(final Set<ClassPathEntry> result) {
    for (Object myClassloader : myClassloaders) {
      URLClassLoader cl = (URLClassLoader) myClassloader;
      // assert cl != null; // see addClassLoader(ClassLoader)
      URL[] urls;
      try {
        urls = cl.getURLs();
        for (URL url : urls) {
          if (!"file".equals(url.getProtocol())) continue;

          String path = fixPath(url.getPath());
          result.add(new ClassPathEntry(path));
        }
      } catch (Exception e) {
        ErrorReporter.info("Exception occurred on trying collect ClassPath URLs. One of possible reasons is shutting down " +
            "Tomcat before finishing tests. Coverage won't be affected but some of uncovered classes could be missing from " +
            "the report.", e);
      }
    }
  }

  private String fixPath(final String path) {
    String result = path;
    try {
      result = URLDecoder.decode(path, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      ErrorReporter.info("Could not decode the path: " + path + ", error: " + e.getMessage(), e);
    }

    if (result.isEmpty()) return result;
    if (result.charAt(0) == '/' && result.length() > 3 && result.charAt(2) == ':') {
      // windows path prefix: /C:/
      result = result.substring(1);
    }
    return result;
  }

  private static Collection<ClassPathEntry> extractEntries(final String classPath) {
    if (classPath == null) return Collections.emptyList();
    String[] entries = classPath.split(System.getProperty("path.separator"));
    Set<ClassPathEntry> result = new HashSet<ClassPathEntry>();
    for (String entry : entries) {
      result.add(new ClassPathEntry(entry));
    }
    return result;
  }

  public Set<ClassLoader> getClassloaders() {
    return myClassloaders;
  }
}
