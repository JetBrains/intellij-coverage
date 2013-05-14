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

  public static Pattern makePattern(String str) {
    return Pattern.compile(str.contains("$") ? str.replace("$", "\\$") : str);
  }

  public static boolean shouldExclude(String className, List excludePatterns) {
    for (Iterator it = excludePatterns.iterator(); it.hasNext(); ) {
      final Pattern excludePattern = (Pattern) it.next();
      if (isInnerClassAware(excludePattern)) {
        // allow explicit inner class filtering
        if (excludePattern.matcher(className).matches()) return true;
      } else {
        // apply exclude patterns to parent class name only - to provide way to filter outer class with all inner classes
        String outerClassName = getOuterClassName(className);
        if (excludePattern.matcher(outerClassName).matches()) return true;
      }
    }
    return false;
  }

  private static boolean isInnerClassAware(Pattern excludePattern) {
    return excludePattern.pattern().contains("$");
  }
}
