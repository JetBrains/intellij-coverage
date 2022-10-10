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
import java.io.IOException;

public class Main {
  public static void main(String[] argsList) {
    try {
      final ReporterArgs args = ReporterArgs.from(argsList);
      final ReportLoadStrategy loadStrategy;
      if (ReporterArgs.RAW_FORMAT.equals(args.format)) {
        loadStrategy = new ReportLoadStrategy.RawReportLoadStrategy(args.reports, args.modules, args.filters);
      } else if (ReporterArgs.AGGREGATED_BINARY_FILE_FORMAT.equals(args.format)) {
        loadStrategy = new ReportLoadStrategy.AggregatedReportLoadStrategy(args.reports, args.modules);
      } else {
        throw new ArgParseException("Unexpected reporter request format: " + args.format);
      }

      final Reporter reporter = new Reporter(loadStrategy);

      boolean fail = false;

      final File xmlFile = args.xmlFile;
      if (xmlFile != null) {
        try {
          reporter.createXMLReport(xmlFile);
        } catch (IOException e) {
          fail = true;
          System.err.println("XML generation failed.");
          e.printStackTrace();
        }
      }

      final File htmlDir = args.htmlDir;
      if (htmlDir != null) {
        try {
          reporter.createHTMLReport(htmlDir, args.title);
        } catch (IOException e) {
          fail = true;
          System.err.println("HTML generation failed.");
          e.printStackTrace();
        }
      }

      if (args.xmlFile == null && args.htmlDir == null) {
        System.err.println("At least one format must be used: XML, HTML.");
        fail = true;
      }

      if (fail) {
        System.exit(1);
      }
    } catch (ArgParseException e) {
      e.printStackTrace(System.err);

      for (String arg : argsList) {
        System.err.println(arg);
      }

      System.err.println();
      System.err.println(ReporterArgs.getHelpString());
      System.exit(1);
    }

  }
}
