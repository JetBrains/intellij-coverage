/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.util.CoverageIOUtil;
import com.intellij.rt.coverage.util.ErrorReporter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class CoverageArgs {
  public File dataFile;
  public boolean testTracking;
  public boolean calcUnloaded;
  public boolean mergeData;
  public boolean branchCoverage;
  public File sourceMap;
  public List<Pattern> includePatterns = new ArrayList<Pattern>();
  public List<Pattern> excludePatterns = new ArrayList<Pattern>();
  public List<Pattern> annotationsToIgnore = new ArrayList<Pattern>();

  public static CoverageArgs fromString(String argsString) throws IllegalArgumentException {
    String[] args;
    // Try to interpret as a file with args first
    File file = new File(argsString);
    if (file.isFile()) {
      try {
        args = readArgsFromFile(file);
      } catch (IOException e) {
        throw new IllegalArgumentException("Failed to read arguments from file: " + file.getAbsolutePath(), e);
      }
    } else {
      args = tokenize(argsString);
    }

    if (args.length < 5) {
      final String usage = "Expected arguments are:\n"
          + "0) data file to save coverage result\n"
          + "1) a flag to enable tracking per test coverage\n"
          + "2) a flag to calculate coverage for unloaded classes\n"
          + "3) a flag to use data file as initial coverage, also use it if several parallel processes are to write into one file\n"
          + "4) a flag to run line coverage or branch coverage otherwise\n";
      throw new IllegalArgumentException("At least 5 arguments expected but " + args.length + " found.\n'" + argsString + "'\n" + usage);
    }
    return fromString(args);
  }

  private static CoverageArgs fromString(String[] args) throws IllegalArgumentException {
    final CoverageArgs result = new CoverageArgs();
    result.dataFile = new File(args[0]);
    result.testTracking = Boolean.parseBoolean(args[1]);
    result.calcUnloaded = Boolean.parseBoolean(args[2]);
    result.mergeData = Boolean.parseBoolean(args[3]);
    result.branchCoverage = !Boolean.parseBoolean(args[4]);

    // This is a side effect to report exceptions accurately
    ErrorReporter.suggestBasePath(result.dataFile.getParent());

    int i = 5;
    if (args.length > 5 && Boolean.parseBoolean(args[5])) {
      result.sourceMap = new File(args[6]);
      i = 7;
    }
    i = readPatterns(result.includePatterns, i, args, "include");

    if (i < args.length && "-exclude".equals(args[i])) {
      i = readPatterns(result.excludePatterns, i + 1, args, "exclude");
    }

    if (i < args.length && "-excludeAnnotations".equals(args[i])) {
      readPatterns(result.annotationsToIgnore, i + 1, args, "exclude annotations");
    }

    return result;
  }

  private static String[] tokenize(String argumentString) {
    List<String> tokenizedArgs = new ArrayList<String>();
    StringBuilder currentArg = new StringBuilder();
    for (int i = 0; i < argumentString.length(); i++) {
      char c = argumentString.charAt(i);
      switch (c) {
        case ' ':
        case ',':
          String arg = currentArg.toString();
          if (!arg.isEmpty()) {
            tokenizedArgs.add(arg);
          }
          currentArg = new StringBuilder();
          break;
        case '\"':
          for (i++; i < argumentString.length(); i++) {
            char d = argumentString.charAt(i);
            if (d == '\"') {
              break;
            }
            currentArg.append(d);
          }
          break;
        default:
          currentArg.append(c);
          break;
      }
    }

    String arg = currentArg.toString();
    if (!arg.isEmpty()) {
      tokenizedArgs.add(arg);
    }
    return tokenizedArgs.toArray(new String[0]);
  }

  private static String[] readArgsFromFile(final File file) throws IOException {
    final List<String> result = new ArrayList<String>();
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
      while (reader.ready()) {
        result.add(reader.readLine());
      }
    } finally {
      CoverageIOUtil.close(reader);
    }
    return result.toArray(new String[0]);
  }

  private static int readPatterns(final List<Pattern> patterns, int i, final String[] args, final String name) throws IllegalArgumentException {
    for (; i < args.length; i++) {
      if (args[i].startsWith("-")) break;
      try {
        patterns.add(Pattern.compile(args[i]));
      } catch (PatternSyntaxException ex) {
        throw new IllegalArgumentException("Problem occurred with " + name + " pattern " + args[i] +
            ". This may cause no tests run and no coverage collected", ex);
      }
    }
    return i;
  }
}
