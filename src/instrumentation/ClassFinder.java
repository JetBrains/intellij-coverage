package com.intellij.rt.coverage.instrumentation;

import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
* @author pavel.sher
*/
public class ClassFinder {
  private List myIncludePatterns;
  private List myExcludePatterns;

  public ClassFinder(final List includePatterns, final List excludePatterns) {
    myIncludePatterns = includePatterns;
    myExcludePatterns = excludePatterns;
  }

  public Collection findMatchedClasses() {
      Set result = new HashSet();
      for (Iterator it = getClassPathEntries().iterator(); it.hasNext();) {
        String entry = (String)it.next();
        ClassPathEntryProcessor p = createEntryProcessor(entry);
        if (p == null) continue;

        try {
          for (Iterator it1 = p.extractClassNames(entry).iterator(); it1.hasNext();) {
            String className = (String)it1.next();
            if (shouldInclude(className)) {
              result.add(className);
            }
          }
        }
        catch (IOException e) {
          e.printStackTrace();
        }
      }
      return result;
    }

    private Collection getClassPathEntries() {
      Set result = new HashSet();

      result.addAll(extractEntries(System.getProperty("java.class.path")));
      result.addAll(extractEntries(System.getProperty("sun.boot.class.path")));
      collectClassloaderEntries(getClass().getClassLoader(), result);
      return result;
    }

    private void collectClassloaderEntries(final ClassLoader cl, final Set result) {
      if (cl == null) return;
      if (cl instanceof URLClassLoader) {
        URL[] urls = ((URLClassLoader)cl).getURLs();
        for (int i=0; i<urls.length; i++) {
          URL url = urls[i];
          if (!"file".equals(url.getProtocol())) continue;

          String path = fixPath(url.getPath());
          if (path != null) {
            result.add(path);
          }
        }
      }

      if (cl.getParent() != null) {
        collectClassloaderEntries(cl.getParent(), result);
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
      return Arrays.asList(entries);
    }

    private boolean shouldInclude(String className) {
      final Perl5Matcher pm = new Perl5Matcher();
      for (Iterator it = myExcludePatterns.iterator(); it.hasNext();) {
        Pattern e = (Pattern)it.next();
        if (pm.matches(className, e)) return false;
      }
      for (Iterator it = myIncludePatterns.iterator(); it.hasNext();) {
        Pattern e = (Pattern)it.next();
        if (pm.matches(className, e)) return true;
      }
      return myIncludePatterns.isEmpty();
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

    private interface ClassPathEntryProcessor {
      Collection extractClassNames(String classPathEntry) throws IOException;
    }

    private static final String CLASS_FILE_SUFFIX = ".class";

    private static class DirectoryEntryProcessor implements ClassPathEntryProcessor {

      public Collection extractClassNames(final String classPathEntry) {
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
    private static class ZipEntryProcessor implements ClassPathEntryProcessor {
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

}
