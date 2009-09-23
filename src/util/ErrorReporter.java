package com.intellij.rt.coverage.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Use this reporter for the cases when exception occurs within coverage engine
 */
public class ErrorReporter {
  private final static String ERROR_FILE = "coverage-error.log";
  private final static SimpleDateFormat myDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

  public static synchronized void reportError(final String message, Throwable t) {
    PrintStream os = null;
    try {
      os = new PrintStream(new FileOutputStream(ERROR_FILE, true));
      StringBuffer buf = new StringBuffer();
      buf.append("[");
      buf.append(myDateFormat.format(new Date()));
      buf.append("] (Coverage): ");
      buf.append(message);

      System.err.println(buf.toString() + ": " + t.toString());
      os.println(buf.toString());

      t.printStackTrace(os);
    } catch (IOException e) {
      System.err.println("Failed to dump stacktrace to error log due to error: " + e.toString());
      System.err.println("Cause: " + t.toString());
    } finally {
      if (os != null) {
        os.close();
      }
    }
  }
}
