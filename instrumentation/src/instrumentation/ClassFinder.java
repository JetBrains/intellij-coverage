package com.intellij.rt.coverage.instrumentation;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.*;

/**
 * @author pavel.sher
 */
public class ClassFinder {
  private List myIncludePatterns;
  private List myExcludePatterns;
  private Set myClassloaders;

  public ClassFinder(final List includePatterns, final List excludePatterns) {
    myIncludePatterns = includePatterns;
    myExcludePatterns = excludePatterns;
    myClassloaders = new HashSet();
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

  public Collection findMatchedClasses() {
    Set classes = new HashSet();
    final Iterator classPathEntryIt = getClassPathEntries().iterator();
    while (classPathEntryIt.hasNext()) {
      final ClassPathEntry entry = (ClassPathEntry) classPathEntryIt.next();
      try {
        classes.addAll(entry.getClassesIterator(myIncludePatterns, myExcludePatterns));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return classes;
  }

  protected Collection getClassPathEntries() {
    Set result = new HashSet();

    result.addAll(extractEntries(System.getProperty("java.class.path")));
    result.addAll(extractEntries(System.getProperty("sun.boot.class.path")));
    collectClassloaderEntries(result);
    return result;
  }

  private void collectClassloaderEntries(final Set result) {
    for (Iterator iterator = myClassloaders.iterator(); iterator.hasNext();) {
      URLClassLoader cl = (URLClassLoader) iterator.next();
      URL[] urls = cl.getURLs();
      for (int i = 0; i < urls.length; i++) {
        URL url = urls[i];
        if (!"file".equals(url.getProtocol())) continue;

        String path = fixPath(url.getPath());
        if (path != null) {
          result.add(new ClassPathEntry(path, cl));
        }
      }
    }
  }

  private String fixPath(final String path) {
    String result = path;
    try {
      result = URLDecoder.decode(path, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    if (result.length() == 0) return result;
    if (result.charAt(0) == '/' && result.length() > 3 && result.charAt(2) == ':') {
      // windows path prefix: /C:/
      result = result.substring(1);
    }
    return result;
  }

  private static Collection extractEntries(final String classPath) {
    if (classPath == null) return Collections.emptyList();
    String[] entries = classPath.split(System.getProperty("path.separator"));
    Set result = new HashSet();
    for (int i = 0; i < entries.length; i++) {
      result.add(new ClassPathEntry(entries[i], null));
    }
    return result;
  }
}
