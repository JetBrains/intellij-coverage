package com.intellij.rt.coverage.util;

/**
 * @author Pavel.Sher
 */
public class ClassNameUtil {
  public static String getOuterClassName(String className) {
    int idx = className.indexOf('$');
    if (idx == -1) return className;
    return className.substring(0, idx);
  }
}
