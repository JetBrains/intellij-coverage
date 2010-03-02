/*
 * User: anna
 * Date: 25-Feb-2010
 */
package com.intellij.rt.coverage.main;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

public class CoveragePremain {
  public static void premain(String argsString, Instrumentation instrumentation) throws Exception {
    final File lib = new File(getArchivePath()).getParentFile();
    final URL[] urls = new URL[5];
    urls[0] = fileToURL(new File(lib, "instrumenter.jar"));
    urls[1] = fileToURL(new File(lib, "asm-commons.jar"));
    urls[2] = fileToURL(new File(lib, "asm-tree-3.0.jar"));
    urls[3] = fileToURL(new File(lib, "asm.jar"));
    urls[4] = fileToURL(new File(lib, "trove4j.jar"));
    System.out.println("Classloader URLs: " + Arrays.asList(urls));
    final Class instrumentator = Class.forName("com.intellij.rt.coverage.instrumentation.Instrumentator", true, new URLClassLoader(urls));
    final Method premainMethod = instrumentator.getDeclaredMethod("premain", new Class[]{String.class, Instrumentation.class});
    premainMethod.invoke(null, new Object[] {argsString, instrumentation});
  }

  private static URL fileToURL(final File file) throws MalformedURLException {
    return file.getAbsoluteFile().toURI().toURL();
  }

  private static String getArchivePath() {
    final String className = CoveragePremain.class.getName().replace('.', '/') + ".class";
    URL resourceURL = CoveragePremain.class.getResource("/" + className);
    if (resourceURL == null) {
      resourceURL = ClassLoader.getSystemResource(className);
    }
    final String fullPath = resourceURL.getFile();
    final int delimiter = fullPath.indexOf("!");
    String archivePath = fullPath.substring(0, delimiter);
    archivePath = removePrefix(archivePath, "file://");
    archivePath = removePrefix(archivePath, "file:/");
    return archivePath;
  }

  private static String removePrefix(String archivePath, String prefix) {
    if (archivePath.startsWith(prefix)) {
      archivePath = archivePath.substring(prefix.length());
    }
    return archivePath;
  }
}