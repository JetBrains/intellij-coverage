package com.intellij.rt.coverage.util;

import java.io.FileNotFoundException;
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

  public static synchronized void reportError(final String message) {
    PrintStream os = null;
    try {
      os = getErrorLogStream();
      StringBuffer buf = prepareMessage(message);

      System.err.println(buf.toString());
      os.println(buf.toString());
    } catch (IOException e) {
      System.err.println("Failed to write to error log file: " + e.toString());
    } finally {
      if (os != null) {
        os.close();
      }
    }
  }

  public static synchronized void reportError(final String message, Throwable t) {
    PrintStream os = null;
    try {
      os = getErrorLogStream();
      StringBuffer buf = prepareMessage(message);

      System.err.println(buf.toString() + ": " + t.toString());
      os.println(buf.toString());

      t.printStackTrace(os);
    } catch (IOException e) {
      System.err.println("Failed to write to error log file: " + e.toString());
      System.err.println("Initial stack trace: " + t.toString());
    } finally {
      if (os != null) {
        os.close();
      }
    }
  }

  public static synchronized void logError(final String message) {
    PrintStream os = null;
    try {
      os = getErrorLogStream();
      StringBuffer buf = prepareMessage(message);
      os.println(buf.toString());
    } catch (IOException e) {
      System.err.println("Failed to write to error log file: " + e.toString());
    } finally {
      if (os != null) {
        os.close();
      }
    }
  }

  private static PrintStream getErrorLogStream() throws FileNotFoundException {
    return new PrintStream(new FileOutputStream(ERROR_FILE, true));
  }

  private static StringBuffer prepareMessage(final String message) {
    StringBuffer buf = new StringBuffer();
    buf.append("[");
    buf.append(myDateFormat.format(new Date()));
    buf.append("] (Coverage): ");
    buf.append(message);
    return buf;
  }
}
