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

package com.intellij.rt.coverage.json;

import com.intellij.rt.coverage.aggregate.Aggregator;
import com.intellij.rt.coverage.aggregate.AggregatorArgs;
import com.intellij.rt.coverage.report.ReporterArgs;
import com.intellij.rt.coverage.report.data.Module;
import com.intellij.rt.coverage.report.util.FileUtils;
import com.intellij.rt.coverage.verify.Verifier;
import com.intellij.rt.coverage.verify.VerifierArgs;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

import static com.intellij.rt.coverage.report.TestUtils.getResourceFile;

public class JsonConfigTest {
  @Test
  public void testReadingJsonFromFile() throws IOException {
    final File jsonFile = getResourceFile("json/reporter.json");
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
  public void testParsingReporterJsonArgs() throws IOException {
    final File jsonFile = getResourceFile("json/reporter.json");
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


  @Test
  public void testParsingAggregatorJsonArgs() throws IOException {
    final File jsonFile = getResourceFile("json/aggregator.json");
    final AggregatorArgs args = AggregatorArgs.parse(jsonFile);
    Assert.assertEquals(3, args.modules.size());

    Assert.assertEquals(2, args.reports.size());
    Assert.assertEquals("ic_path1", args.reports.get(0).getDataFile().getPath());
    Assert.assertEquals("smap_path1", args.reports.get(0).getSourceMapFile().getPath());
    Assert.assertEquals("ic_path2", args.reports.get(1).getDataFile().getPath());
    Assert.assertNull(args.reports.get(1).getSourceMapFile());


    final Module module1 = args.modules.get(0);
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

    Assert.assertEquals(2, args.requests.size());
    final Aggregator.Request request1 = args.requests.get(0);
    Assert.assertEquals("aggregatedPath", request1.outputFile.getPath());

    final Aggregator.Request request2 = args.requests.get(1);
    Assert.assertEquals("aggregatedPath", request2.outputFile.getPath());
    Assert.assertEquals("smapPath", request2.smapFile.getPath());
  }


  @Test
  public void testParsingVerifierJsonArgs() throws IOException {
    final File jsonFile = getResourceFile("json/verifier.json");
    final VerifierArgs args = VerifierArgs.parse(jsonFile);

    Assert.assertEquals(2, args.rules.size());

    final Verifier.Rule rule1 = args.rules.get(0);
    Assert.assertEquals(1, rule1.id);
    Assert.assertEquals(Verifier.Target.ALL, rule1.target);
    Assert.assertEquals("path1", rule1.reportFile.getPath());
    Assert.assertEquals(1, rule1.bounds.size());
    final Verifier.Bound bound1 = rule1.bounds.get(0);
    Assert.assertEquals(1, bound1.id);
    Assert.assertEquals(Verifier.Counter.LINE, bound1.counter);
    Assert.assertEquals(Verifier.ValueType.COVERED, bound1.valueType);
    Assert.assertEquals(BigDecimal.valueOf(100), bound1.min);
    Assert.assertNull(bound1.max);

    final Verifier.Rule rule2 = args.rules.get(1);
    Assert.assertEquals(2, rule2.id);
    Assert.assertEquals(Verifier.Target.PACKAGE, rule2.target);
    Assert.assertEquals("path2", rule2.reportFile.getPath());
    Assert.assertEquals(1, rule2.bounds.size());
    final Verifier.Bound bound2 = rule2.bounds.get(0);
    Assert.assertEquals(1, bound2.id);
    Assert.assertEquals(Verifier.Counter.LINE, bound2.counter);
    Assert.assertEquals(Verifier.ValueType.MISSED_RATE, bound2.valueType);
    Assert.assertEquals(BigDecimal.valueOf(0.111111), bound2.max);
    Assert.assertEquals(BigDecimal.valueOf(0.000001), bound2.min);

    Assert.assertEquals("verify.json", args.resultFile.getPath());
  }
}
