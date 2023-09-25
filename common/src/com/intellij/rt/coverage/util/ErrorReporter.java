/*
 * Copyright 2000-2022 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.rt.coverage.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Use this reporter for the cases when exception occurs within coverage engine
 */
public class ErrorReporter {
  public static final String ERROR_FILE = "coverage-error.log";
  public static final String LOG_LEVEL_SYSTEM_PROPERTY = "idea.coverage.log.level";
  private static final SimpleDateFormat myDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
  public static final int DEBUG = 0;
  public static final int INFO = 1;
  public static final int WARNING = 2;
  public static final int ERROR = 3;

  private static String basePath;
  private static int myLogLevel = WARNING;

  static {
    setUpLogLevel();
  }

  public static void setBasePath(String path) {
    basePath = path;
  }

  public static void setLogLevel(int level) {
    myLogLevel = level;
  }


  public static void error(String message) {
    log(ERROR, message, null);
  }

  public static void error(String message, Throwable t) {
    log(ERROR, message, t);
  }

  public static void warn(String message) {
    log(WARNING, message, null);
  }

  public static void warn(String message, Throwable t) {
    log(WARNING, message, t);
  }

  public static void info(String message) {
    log(INFO, message, null);
  }

  public static void info(String message, Throwable t) {
    log(INFO, message, t);
  }

  public static void debug(String message) {
    log(DEBUG, message, null);
  }

  public static void printInfo(String message) {
    if (myLogLevel > INFO) return;
    System.out.println(message);
  }

  private static synchronized void log(int level, String message, Throwable t) {
    String prefix = logPrefix(level);

    PrintStream os = null;
    try {
      os = getErrorLogStream();
      printLogMessage(os, prefix, message, t);

      if (level >= myLogLevel) {
        String consoleMessage = getConsoleMessage(prefix, message, t);
        System.err.println(consoleMessage);
      }
    } catch (IOException ignored) {
    } finally {
      CoverageIOUtil.close(os);
    }
  }

  private static PrintStream getErrorLogStream() throws FileNotFoundException {
    return new PrintStream(new FileOutputStream(basePath != null ? new File(basePath, ERROR_FILE) : new File(ERROR_FILE), true));
  }

  private static String logPrefix(int level) {
    String levelString = "";
    switch (level) {
      case DEBUG:
        levelString = "DEBUG";
        break;
      case INFO:
        levelString = " INFO";
        break;
      case WARNING:
        levelString = " WARN";
        break;
      case ERROR:
        levelString = "ERROR";
        break;
    }
    return '[' + myDateFormat.format(new Date()) + "] (Coverage " + levelString + "): ";
  }

  private static String getConsoleMessage(String prefix, String message, Throwable t) {
    StringBuilder buf = new StringBuilder();
    buf.append(prefix);
    if (message != null) buf.append(message);
    if (t != null) {
      if (message != null) buf.append(": ");
      buf.append(t);
    }
    return buf.toString();
  }

  private static void printLogMessage(PrintStream out, String prefix, String message, Throwable t) {
    if (message != null) {
      out.println(prefix + message);
    } else {
      out.println(prefix);
    }
    if (t != null) {
      t.printStackTrace(out);
    }
  }

  private static void setUpLogLevel() {
    String logLevelString = System.getProperty(LOG_LEVEL_SYSTEM_PROPERTY);
    int logLevel;
    if ("error".equals(logLevelString)) {
      logLevel = ERROR;
    } else if ("warn".equals(logLevelString)) {
      logLevel = WARNING;
    } else if ("info".equals(logLevelString)) {
      logLevel = INFO;
    } else if ("debug".equals(logLevelString)) {
      logLevel = DEBUG;
    } else {
      return;
    }
    setLogLevel(logLevel);
  }
}
