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

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;

public class ReporterArgsTest {
  public static File argsToFile(ReporterArgs args) throws IOException {
    final File argsFile = File.createTempFile("args", ".txt");
    argsToFile(args, argsFile);
    return argsFile;
  }

  public static void argsToFile(ReporterArgs args, File argsFile) throws FileNotFoundException {
    final PrintWriter printer = new PrintWriter(argsFile);

    for (BinaryReport report : args.reports) {
      printer.println(report.getDataFile().getPath());
      printer.println(report.getSourceMapFile().getPath());
    }
    printer.println();

    for (File source : args.sources) {
      printer.println(source.getPath());
    }
    printer.println();

    for (File output : args.outputs) {
      printer.println(output.getPath());
    }
    printer.println();

    printer.println(args.xmlFile != null ? args.xmlFile.getPath() : "");
    printer.println(args.htmlDir != null ? args.htmlDir.getPath() : "");

    printer.flush();
    printer.close();
  }

  @Test
  public void testArgs() throws Exception {
    final ReporterArgs tmpArgs = new ReporterArgs(Collections.singletonList(new BinaryReport(new File("test.ic"), new File("test.smap"))),
        Collections.singletonList(new File("a/")), Collections.singletonList(new File("out")), new File("a.xml"), new File("html/"));

    final ReporterArgs args = ReporterArgs.parse(argsToFile(tmpArgs));
    final List<BinaryReport> reports = args.reports;
    Assert.assertEquals(1, reports.size());
    final BinaryReport report = reports.get(0);
    Assert.assertEquals("test.ic", report.getDataFile().getName());
    Assert.assertEquals("test.smap", report.getSourceMapFile().getName());
    final List<File> output = args.outputs;
    Assert.assertEquals(1, output.size());
    for (File f : output) {
      Assert.assertTrue(f.getName().startsWith("out"));
    }
    final List<File> sources = args.sources;
    Assert.assertEquals(1, output.size());
    for (File f : sources) {
      Assert.assertTrue(f.getName().startsWith("a"));
    }

    final File xmlFile = args.xmlFile;
    Assert.assertNotNull(xmlFile);
    final File htmlDir = args.htmlDir;
    Assert.assertNotNull(htmlDir);
  }

  @Test(expected = ReporterArgs.ArgParseException.class)
  public void testAbsentDataFile() throws Exception {
    final ReporterArgs args = new ReporterArgs(Collections.singletonList(new BinaryReport(new File(""), new File("a.smap"))),
        Collections.singletonList(new File("a/")), Collections.singletonList(new File("a/")), new File("a.xml"), null);
    ReporterArgs.parse(argsToFile(args));
  }

  @Test(expected = ReporterArgs.ArgParseException.class)
  public void testAbsentOutput() throws Exception {
    final ReporterArgs args = new ReporterArgs(Collections.singletonList(new BinaryReport(new File("a.ic"), new File("a.smap"))),
        Collections.singletonList(new File("a/")), Collections.<File>emptyList(), new File("a.xml"), null);
    ReporterArgs.parse(argsToFile(args));
  }

  @Test
  public void testHelp() {
    Assert.assertNotNull(ReporterArgs.getHelpString());
  }

}
