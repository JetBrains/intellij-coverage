/*
 * User: anna
 * Date: 02-Mar-2010
 */
package com.intellij.rt.coverage.util;

import java.io.File;
import java.net.URL;

public class URLsUtil {
  public static final String FILE = "file";
  public static final String PROTOCOL_DELIMITER = ":";
  public static final String JAR_DELIMITER = "!";

  public static boolean startsWithChar(CharSequence s, char prefix) {
return s != null && s.length() != 0 && s.charAt(0) == prefix;
}

  public static String extractRoot(URL resourceURL, String resourcePath) {
   if (!(startsWithChar(resourcePath, '/') || startsWithChar(resourcePath, '\\'))) {
     //noinspection HardCodedStringLiteral
     System.err.println("precondition failed: "+resourcePath);
     return null;
   }
   String protocol = resourceURL.getProtocol();
   String resultPath = null;

   if (FILE.equals(protocol)) {
     String path = resourceURL.getFile();
     final String testPath = path.replace('\\', '/');
     final String testResourcePath = resourcePath.replace('\\', '/');
     if (endsWithIgnoreCase(testPath, testResourcePath)) {
       resultPath = path.substring(0, path.length() - resourcePath.length());
     }
   }
   else if ("jar".equals(protocol)) {
     String fullPath = resourceURL.getFile();
     int delimiter = fullPath.indexOf(JAR_DELIMITER);
     if (delimiter >= 0) {
       String archivePath = fullPath.substring(0, delimiter);
       if (startsWithConcatenationOf(archivePath, FILE, PROTOCOL_DELIMITER)) {
         resultPath = archivePath.substring(FILE.length() + PROTOCOL_DELIMITER.length());
       }
     }
   }
   if (resultPath == null) {
     //noinspection HardCodedStringLiteral
     System.err.println("cannot extract: "+resultPath + " from "+resourceURL);
     return null;
   }

    if (resourcePath.endsWith(File.separator)) {
      resultPath = resultPath.substring(0, resultPath.lastIndexOf(File.separator));
    }

   return resultPath;
 }

  public static boolean startsWithConcatenationOf(String testee, String firstPrefix, String secondPrefix) {
   int l1 = firstPrefix.length();
   int l2 = secondPrefix.length();
   if (testee.length() < l1 + l2) return false;
   return testee.startsWith(firstPrefix) && testee.regionMatches(l1, secondPrefix, 0, l2);
 }

  public static boolean endsWithIgnoreCase(String str, String suffix) {
   final int stringLength = str.length();
   final int suffixLength = suffix.length();
   return stringLength >= suffixLength && str.regionMatches(true, stringLength - suffixLength, suffix, 0, suffixLength);
 }
}