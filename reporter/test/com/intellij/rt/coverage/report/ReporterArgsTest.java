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

import com.intellij.rt.coverage.report.data.BinaryReport;
import com.intellij.rt.coverage.report.data.Module;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class ReporterArgsTest {
  public static File argsToFile(BinaryReport binaryReport, String outputPath, String sourcesPath, String xmlPath, String htmlPath) throws IOException {
    final JSONObject args = new JSONObject();
    final JSONObject module = new JSONObject();
    final JSONObject report = new JSONObject();
    report.put(ReporterArgs.IC_FILE_TAG, binaryReport.getDataFile().getAbsolutePath());
    report.put(ReporterArgs.SMAP_FILE_TAG, binaryReport.getSourceMapFile().getAbsolutePath());
    module.append(ReporterArgs.REPORTS_TAG, report);
    module.append(ReporterArgs.OUTPUTS_TAG, outputPath);
    module.append(ReporterArgs.SOURCES_TAG, sourcesPath);
    args.append(ReporterArgs.MODULES_TAG, module);

    if (htmlPath != null) {
      args.put(ReporterArgs.HTML_DIR_TAG, htmlPath);
    }
    if (xmlPath != null) {
      args.put(ReporterArgs.XML_FILE_TAG, xmlPath);
    }
    final File argsFile = File.createTempFile("args", ".txt");
    final Writer writer = new FileWriter(argsFile);
    writer.write(args.toString());
    writer.close();
    return argsFile;
  }

  @Test
  public void testArgs() throws Exception {
    final ReporterArgs args = ReporterArgs.parse(argsToFile(new BinaryReport(new File("test.ic"), new File("test.smap")), "out", "a/", "a.xml", "html/"));
    Assert.assertEquals(1, args.modules.size());
    final Module module = args.modules.get(0);
    final List<BinaryReport> reports = module.getReports();
    Assert.assertEquals(1, reports.size());
    final BinaryReport report = reports.get(0);
    Assert.assertEquals("test.ic", report.getDataFile().getName());
    Assert.assertEquals("test.smap", report.getSourceMapFile().getName());
    final List<File> output = module.getOutputRoots();
    Assert.assertEquals(1, output.size());
    for (File f : output) {
      Assert.assertTrue(f.getName().startsWith("out"));
    }
    final List<File> sources = module.getSources();
    Assert.assertEquals(1, output.size());
    for (File f : sources) {
      Assert.assertTrue(f.getName().startsWith("a"));
    }

    final File xmlFile = args.xmlFile;
    Assert.assertNotNull(xmlFile);
    final File htmlDir = args.htmlDir;
    Assert.assertNotNull(htmlDir);
  }

  @Test
  public void testHelp() {
    Assert.assertNotNull(ReporterArgs.getHelpString());
  }

}
