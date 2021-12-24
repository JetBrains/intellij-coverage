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

import com.intellij.rt.coverage.report.data.Module;
import com.intellij.rt.coverage.report.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static com.intellij.rt.coverage.report.TestUtils.getResourceFile;

public class JsonConfigTest {
  @Test
  public void testReadingJsonFromFile() throws IOException {
    final File jsonFile = getResourceFile("json_config.txt");
    final String json = FileUtils.readAll(jsonFile);
    final JSONObject args = new JSONObject(json);
    Assert.assertNotNull(args);

    Assert.assertEquals("path", args.getString("xml"));
    Assert.assertEquals("directory", args.getString("html"));

    final JSONArray modules = args.getJSONArray("modules");
    Assert.assertEquals(3, modules.length());
    final JSONObject module1 = modules.getJSONObject(0);

    Assert.assertEquals(1, args.getJSONArray("reports").length());
    Assert.assertEquals("icPath", args.getJSONArray("reports").getJSONObject(0).getString("ic"));
    Assert.assertEquals("smapPath", args.getJSONArray("reports").getJSONObject(0).getString("smap"));
    Assert.assertEquals(1, module1.getJSONArray("output").length());
    Assert.assertEquals("outputPath", module1.getJSONArray("output").getString(0));
    Assert.assertEquals(1, module1.getJSONArray("sources").length());
    Assert.assertEquals("sourcePath", module1.getJSONArray("sources").getString(0));

    final JSONObject module2 = modules.getJSONObject(1);
    Assert.assertEquals(2, module2.getJSONArray("output").length());
    Assert.assertEquals("outputPath1", module2.getJSONArray("output").getString(0));
    Assert.assertEquals("outputPath2", module2.getJSONArray("output").getString(1));
    Assert.assertFalse(module2.has("sources"));
  }

  @Test
  public void testParsingJsonArgs() throws IOException {
    final File jsonFile = getResourceFile("json_config.txt");
    final ReporterArgs args = ReporterArgs.parse(jsonFile);
    Assert.assertEquals(3, args.modules.size());
    Assert.assertEquals("path", args.xmlFile.getPath());
    Assert.assertEquals("directory", args.htmlDir.getPath());

    final Module module1 = args.modules.get(0);
    Assert.assertEquals(1, args.reports.size());
    Assert.assertEquals("icPath", args.reports.get(0).getDataFile().getPath());
    Assert.assertEquals("smapPath", args.reports.get(0).getSourceMapFile().getPath());
    Assert.assertEquals(1, module1.getOutputRoots().size());
    Assert.assertEquals("outputPath", module1.getOutputRoots().get(0).getPath());
    Assert.assertEquals(1, module1.getSources().size());
    Assert.assertEquals("sourcePath", module1.getSources().get(0).getPath());

    final Module module2 = args.modules.get(1);
    Assert.assertEquals(2, module2.getOutputRoots().size());
    Assert.assertEquals("outputPath1", module2.getOutputRoots().get(0).getPath());
    Assert.assertEquals("outputPath2", module2.getOutputRoots().get(1).getPath());
    Assert.assertEquals(0, module2.getSources().size());

    final Module module3 = args.modules.get(2);
    Assert.assertEquals(2, module3.getSources().size());
    Assert.assertEquals("sourcePath1", module3.getSources().get(0).getPath());
    Assert.assertEquals("sourcePath2", module3.getSources().get(1).getPath());
    Assert.assertEquals(0, module3.getOutputRoots().size());
  }
}
