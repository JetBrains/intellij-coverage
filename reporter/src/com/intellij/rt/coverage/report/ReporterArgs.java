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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ReporterArgs {
  public final List<BinaryReport> reports;
  public final List<File> sources;
  public final List<File> outputs;
  public final File xmlFile;
  public final File htmlDir;

  ReporterArgs(List<BinaryReport> reports, List<File> sources, List<File> outputs, File xmlFile, File htmlDir) {
    this.reports = reports;
    this.sources = sources;
    this.outputs = outputs;
    this.xmlFile = xmlFile;
    this.htmlDir = htmlDir;
  }

  public static ReporterArgs from(String[] args) throws ArgParseException {
    if (args.length != 1) {
      throw new ArgParseException("Single file name argument expected, but " + args.length + " arguments found.");
    }
    final String fileName = args[0];
    final File argsFile = new File(fileName);
    if (!(argsFile.exists() && argsFile.isFile())) {
      throw new ArgParseException("Expected file with arguments, but " + fileName + " is not a file.");
    }
    try {
      return parse(argsFile);
    } catch (FileNotFoundException e) {
      throw new ArgParseException(e.getMessage());
    }
  }

  public static ReporterArgs parse(File argsFile) throws FileNotFoundException, ArgParseException {
    final Scanner scanner = new Scanner(argsFile, "UTF-8");

    final List<BinaryReport> binaries = new ArrayList<BinaryReport>();
    final List<File> sources = new ArrayList<File>();
    final List<File> outputs = new ArrayList<File>();
    File xmlFile = null;
    File htmlFile = null;

    String line = scanner.nextLine();
    while (!line.isEmpty()) {
      final File dataFile = new File(line);
      line = scanner.nextLine();
      if (line.isEmpty()) {
        throw new ArgParseException("SMAP file is required, but not found for " + dataFile.getAbsolutePath() + " report.");
      }
      final File smapFile = new File(line);
      binaries.add(new BinaryReport(dataFile, smapFile));
      line = scanner.nextLine();
    }

    line = scanner.nextLine();
    while (!line.isEmpty()) {
      sources.add(new File(line));
      line = scanner.nextLine();
    }

    line = scanner.nextLine();
    while (!line.isEmpty()) {
      outputs.add(new File(line));
      line = scanner.nextLine();
    }

    line = scanner.nextLine();
    if (!line.isEmpty()) {
      xmlFile = new File(line);
    }

    line = scanner.nextLine();
    if (!line.isEmpty()) {
      htmlFile = new File(line);
    }

    if (binaries.isEmpty()) {
      throw new ArgParseException("Empty list of reports.");
    }
    if (outputs.isEmpty()) {
      throw new ArgParseException("Empty list of outputs.");
    }
    if (htmlFile != null && sources.isEmpty()) {
      throw new ArgParseException("Sources list is required for html report.");
    }

    return new ReporterArgs(binaries, sources, outputs, xmlFile, htmlFile);
  }

  public static String getHelpString() {
    return "All arguments are divided by new line. Absence of an argument is presented as new line.\n" +
        "Arguments list:" +
        "1) Reports. Each report consists of data file and smap file (two lines). There may be several reports, list of them must end with new line.\n" +
        "2) Sources list. Ends with new line. Must be non empty for html report.\n" +
        "3) Outputs list. Ends with new line. Optional.\n" +
        "4) XML output file or empty line." +
        "5) HTML output dir or empty line.";
  }

  public static class ArgParseException extends Exception {
    ArgParseException(String message) {
      super(message);
    }
  }
}
