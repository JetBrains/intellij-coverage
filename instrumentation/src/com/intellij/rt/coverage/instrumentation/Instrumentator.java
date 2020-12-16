/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.util.ErrorReporter;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Instrumentator {
  public static void premain(String argsString, Instrumentation instrumentation) throws Exception {
    new Instrumentator().performPremain(argsString, instrumentation);
  }

  public void performPremain(String argsString, Instrumentation instrumentation) throws Exception {
    String[] args;
    if (argsString != null) {
      File argsFile = new File(argsString);
      if (argsFile.isFile()) {
        try {
          args = readArgsFromFile(argsString);
        } catch (IOException e) {
          ErrorReporter.reportError("Arguments were not passed correctly", e);
          return;
        }
      } else {
        args = tokenize(argsString);
      }
    } else {
      ErrorReporter.reportError("Argument string should be passed");
      return;
    }

    if (0 < args.length && args.length < 5) {
      System.err.println("At least 5 arguments expected but " + args.length + " found.\n"
          + "Expected arguments are:\n"
          + "1) data file to save coverage result\n"
          + "2) a flag to enable tracking per test coverage\n"
          + "3) a flag to calculate coverage for unloaded classes\n"
          + "4) a flag to use data file as initial coverage\n"
          + "5) a flag to run coverage in sampling mode or in tracing mode otherwise\n");
      System.exit(1);
    }

    final File dataFile = args.length > 0 ? new File(args[0]) : null;
    final boolean traceLines = args.length > 0 && Boolean.parseBoolean(args[1]);
    final boolean calcUnloaded = args.length > 0 && Boolean.parseBoolean(args[2]);
    final ProjectData initialData = args.length > 0 && Boolean.parseBoolean(args[3]) && dataFile.isFile()
        ? ProjectDataLoader.load(dataFile) : null;
    final boolean sampling = args.length == 0 || Boolean.parseBoolean(args[4]);
    if (dataFile != null) {
      ErrorReporter.setBasePath(dataFile.getParent());
    }
    int i = 5;

    final File sourceMapFile;
    if (args.length > 5 && Boolean.parseBoolean(args[5])) {
      sourceMapFile = new File(args[6]);
      i = 7;
    } else {
      sourceMapFile = null;
    }

    final ProjectData data = ProjectData.createProjectData(dataFile, initialData, traceLines, sampling);
    final List<Pattern> includePatterns = new ArrayList<Pattern>();
    System.out.println("---- IntelliJ IDEA coverage runner ---- ");
    System.out.println(sampling ? "sampling ..." : ("tracing " + (traceLines ? "and tracking per test coverage ..." : "...")));
    final String excludes = "-exclude";
    System.out.println("include patterns:");
    for (; i < args.length; i++) {
      if (excludes.equals(args[i])) break;
      try {
        includePatterns.add(Pattern.compile(args[i]));
        System.out.println(args[i]);
      } catch (PatternSyntaxException ex) {
        System.err.println("Problem occurred with include pattern " + args[i]);
        System.err.println(ex.getDescription());
        System.err.println("This may cause no tests run and no coverage collected");
        System.exit(1);
      }
    }
    System.out.println("exclude patterns:");
    i++;
    final List<Pattern> excludePatterns = new ArrayList<Pattern>();
    for (; i < args.length; i++) {
      try {
        final Pattern pattern = Pattern.compile(args[i]);
        excludePatterns.add(pattern);
        System.out.println(pattern.pattern());
      } catch (PatternSyntaxException ex) {
        System.err.println("Problem occurred with exclude pattern " + args[i]);
        System.err.println(ex.getDescription());
        System.err.println("This may cause no tests run and no coverage collected");
        System.exit(1);
      }
    }

    final ClassFinder cf = new ClassFinder(includePatterns, excludePatterns);
    if (dataFile != null) {
      final SaveHook hook = new SaveHook(dataFile, calcUnloaded, cf);
      hook.setSourceMapFile(sourceMapFile);
      Runtime.getRuntime().addShutdownHook(new Thread(hook));
    }

    final boolean shouldCalculateSource = sourceMapFile != null;
    instrumentation.addTransformer(new CoverageClassfileTransformer(data, shouldCalculateSource, excludePatterns, includePatterns, cf));
  }

  private String[] readArgsFromFile(String arg) throws IOException {
    final List<String> result = new ArrayList<String>();
    final File file = new File(arg);
    final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
    try {
      while (reader.ready()) {
        result.add(reader.readLine());
      }
    } finally {
      reader.close();
    }
    return result.toArray(new String[0]);
  }

  private static String[] tokenize(String argumentString) {
    List<String> tokenizedArgs = new ArrayList<String>();
    StringBuffer currentArg = new StringBuffer();
    for (int i = 0; i < argumentString.length(); i++) {
      char c = argumentString.charAt(i);
      switch (c) {
        default:
          currentArg.append(c);
          break;
        case ' ':
          String arg = currentArg.toString();
          if (arg.length() > 0) {
            tokenizedArgs.add(arg);
          }
          currentArg = new StringBuffer();
          break;
        case '\"':
          for (i++; i < argumentString.length(); i++) {
            char d = argumentString.charAt(i);
            if (d == '\"') {
              break;
            }
            currentArg.append(d);
          }
      }
    }

    String arg = currentArg.toString();
    if (arg.length() > 0) {
      tokenizedArgs.add(arg);
    }
    return tokenizedArgs.toArray(new String[0]);
  }
}
