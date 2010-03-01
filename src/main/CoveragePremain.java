/*
 * User: anna
 * Date: 25-Feb-2010
 */
package com.intellij.rt.coverage.main;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class CoveragePremain {
  public static void premain(String argsString, Instrumentation instrumentation) throws Exception {
    final File lib = new File(getArchivePath()).getParentFile();
    final URL[] urls = new URL[5];
    urls[0] = new File(lib, "instrumenter.jar").toURI().toURL();
    urls[1] = new File(lib, "asm-commons.jar").toURI().toURL();
    urls[2] = new File(lib, "asm-tree-3.0.jar").toURI().toURL();
    urls[3] = new File(lib, "asm.jar").toURI().toURL();
    urls[4] = new File(lib, "trove4j.jar").toURI().toURL();
    final Class instrumentator = Class.forName("com.intellij.rt.coverage.instrumentation.Instrumentator", true, new URLClassLoader(urls));
    final Method premainMethod = instrumentator.getDeclaredMethod("premain", new Class[]{String.class, Instrumentation.class});
    premainMethod.invoke(null, new Object[] {argsString, instrumentation});
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