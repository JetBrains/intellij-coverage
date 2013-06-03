package com.intellij.rt.coverage.util;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Pavel.Sher
 */
public class ClassNameUtil {
  public static String getOuterClassName(String className) {
    int idx = className.indexOf('$');
    if (idx == -1) return className;
    return className.substring(0, idx);
  }

  public static boolean shouldExclude(String className, List excludePatterns) {
    for (Iterator it = excludePatterns.iterator(); it.hasNext(); ) {
      final Pattern excludePattern = (Pattern) it.next();
      if (excludePattern.matcher(className).matches()) return true;
    }
    return false;
  }
}
