/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package com.intellij.rt.coverage.report;

import java.io.File;
import java.util.*;

public class ReporterArgs {
  private static final String REPORTS_ARG = "reports";
  private static final String XML_FILE_ARG = "xml";
  private static final String HTML_DIR_ARG = "html";
  private static final String SOURCE_DIRS_ARG = "sources";
  private static final String OUTPUT_DIRS_ARG = "output";

  private static final List<String> ourAcceptableArgs = Arrays.asList(
      REPORTS_ARG, XML_FILE_ARG, HTML_DIR_ARG, SOURCE_DIRS_ARG, OUTPUT_DIRS_ARG);

  private static final char ARG_VALUE_DELIMITER = '=';
  private static final char LIST_DELIMITER = ',';
  private static final char PAIR_DELIMITER = ':';
  private static final char QUOTES = '\"';

  private final Map<String, String> myArgs;

  private ReporterArgs(String[] args) throws ArgParseException {
    myArgs = parseArgs(args);
  }

  public static ReporterArgs from(String[] args) throws ArgParseException {
    return new ReporterArgs(args);
  }

  public static String getHelpString() {
    return "Reporter args help:\n"
        + REPORTS_ARG + ARG_VALUE_DELIMITER + "<List of pairs <Path to ic file>" + PAIR_DELIMITER + "<Path to source map file> separated with " + LIST_DELIMITER + ">  (obligatory, at least one pair)\n"
        + XML_FILE_ARG + ARG_VALUE_DELIMITER + "<Path to xml file> - generate xml report\n"
        + HTML_DIR_ARG + ARG_VALUE_DELIMITER + "<Path to html directory> - generate html report\n"
        + SOURCE_DIRS_ARG + ARG_VALUE_DELIMITER + "<List of paths to source root directories separated with " + LIST_DELIMITER + "> (obligatory for html)\n"
        + OUTPUT_DIRS_ARG + ARG_VALUE_DELIMITER + "<List of paths to output root directories separated with " + LIST_DELIMITER + "> (obligatory)\n"
        + "\n"
        + "A file path may be wrapped with quotes (" + QUOTES + ") to handle spaces.\n";
  }

  private static Map<String, String> parseArgs(String[] args) throws ArgParseException {
    final Map<String, String> result = new HashMap<String, String>();
    for (String arg : args) {
      final int index = arg.indexOf(ARG_VALUE_DELIMITER);
      if (index > 0) {
        final String argName = arg.substring(0, index);
        final String argValue = arg.substring(index + 1);
        if (!ourAcceptableArgs.contains(argName)) {
          throw new ArgParseException("Unacceptable argument: " + argName);
        }
        result.put(argName, argValue);
      } else {
        throw new ArgParseException("Unrecognized option: " + arg);
      }
    }
    return result;
  }

  public List<BinaryReport> getReports() throws ArgParseException {
    final String args = getString(REPORTS_ARG, true);
    final List<BinaryReport> reports = new ArrayList<BinaryReport>();
    for (String pair : getStrings(args)) {
      final List<String> paths = getPair(pair);
      reports.add(new BinaryReport(getFile(paths.get(0)), getFile(paths.get(1))));
    }
    return reports;
  }

  public File getXmlFile() throws ArgParseException {
    return getFile(getString(XML_FILE_ARG, false));
  }

  public File getHtmlDir() throws ArgParseException {
    return getFile(getString(HTML_DIR_ARG, false));
  }

  public List<File> getSourceDirs() throws ArgParseException {
    final String arg = getString(SOURCE_DIRS_ARG, getString(HTML_DIR_ARG, false) != null);
    return getFiles(arg);
  }

  public List<File> getOutputDirs() throws ArgParseException {
    final String arg = getString(OUTPUT_DIRS_ARG, true);
    return getFiles(arg);
  }

  private List<File> getFiles(String args) {
    final List<File> result = new ArrayList<File>();
    for (String path : getStrings(args)) {
      result.add(getFile(path));
    }
    return result;
  }

  private List<String> getStrings(String args) {
    return split(args, LIST_DELIMITER);
  }

  // internal for test
  List<String> getPair(String arg) throws ArgParseException {
    final List<String> result = split(arg, PAIR_DELIMITER);
    if (result.size() != 2) {
      throw new ArgParseException("Pair must contain 2 elements separated with '" + PAIR_DELIMITER + "':" + arg);
    }
    return result;
  }

  private String getString(String name, boolean obligatory) throws ArgParseException {
    final String result = myArgs.get(name);
    if (obligatory && result == null) {
      throw new ArgParseException("Argument is missed: " + name);
    }
    return myArgs.get(name);
  }

  /**
   * Path to a file may be in quotes.
   */
  private File getFile(String path) {
    if (path == null) return null;
    if (path.length() > 2 && path.charAt(0) == QUOTES && path.charAt(path.length() - 1) == QUOTES) {
      path = path.substring(1, path.length() - 1);
    }
    return new File(path);
  }

  private List<String> split(String arg, char delimiter) {
    final List<String> result = new ArrayList<String>();
    StringBuilder builder = new StringBuilder();
    boolean quoted = false;
    for (int i = 0; i < arg.length(); i++) {
      final char c = arg.charAt(i);
      if (c == QUOTES) quoted = !quoted;
      if (!quoted && c == delimiter) {
        result.add(builder.toString());
        builder = new StringBuilder();
        continue;
      }
      builder.append(c);
    }
    result.add(builder.toString());
    return result;
  }

  public static class ArgParseException extends Exception {
    ArgParseException(String message) {
      super(message);
    }
  }
}
