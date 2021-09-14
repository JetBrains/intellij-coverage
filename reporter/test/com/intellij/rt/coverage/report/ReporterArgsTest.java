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
import java.util.List;

public class ReporterArgsTest {
  @Test
  public void testRequiredArgs() throws Exception {
    final ReporterArgs args = ReporterArgs.from(new String[]{"datafile=test.ic", "smapfile=test.smap", "output=out1,out2"});
    Assert.assertNotNull(args.getDataFile());
    Assert.assertNotNull(args.getSourceMapFile());
    Assert.assertEquals("test.ic", args.getDataFile().getName());
    Assert.assertEquals("test.smap", args.getSourceMapFile().getName());
    final List<File> output = args.getOutputDirs();
    Assert.assertNotNull(output);
    Assert.assertEquals(2, output.size());
    for (File f : output) {
      Assert.assertTrue(f.getName().startsWith("out"));
    }
  }

  @Test(expected = ReporterArgs.ArgParseException.class)
  public void testAbsentDataFile() throws Exception {
    final ReporterArgs args = ReporterArgs.from(new String[]{"smapfile=test.smap"});
    Assert.assertNotNull(args.getSourceMapFile());
    args.getDataFile();
  }

  @Test(expected = ReporterArgs.ArgParseException.class)
  public void testAbsentSourceMapFile() throws Exception {
    final ReporterArgs args = ReporterArgs.from(new String[]{"datafile=test.ic"});
    Assert.assertNotNull(args.getDataFile());
    args.getSourceMapFile();
  }

  @Test(expected = ReporterArgs.ArgParseException.class)
  public void testAbsentOutput() throws Exception {
    final ReporterArgs args = ReporterArgs.from(new String[]{"datafile=test.ic", "smapfile=test.smap"});
    Assert.assertNotNull(args.getDataFile());
    Assert.assertNotNull(args.getSourceMapFile());
    args.getOutputDirs();
  }

  @Test(expected = ReporterArgs.ArgParseException.class)
  public void testUnacceptableArgument() throws Exception {
    ReporterArgs.from(new String[]{"datafile=test.ic", "smapfile=test.smap", "unknown=42"});
  }

  @Test
  public void testXMLArgs() throws Exception {
    final ReporterArgs args = ReporterArgs.from(new String[]{"datafile=test.ic", "smapfile=test.smap", "xml=test.xml"});
    Assert.assertNotNull(args.getDataFile());
    Assert.assertNotNull(args.getSourceMapFile());
    Assert.assertNotNull(args.getXmlFile());
    Assert.assertEquals("test.xml", args.getXmlFile().getName());
  }

  @Test
  public void testHTMLArgs() throws Exception {
    final ReporterArgs args = ReporterArgs.from(new String[]{"datafile=test.ic", "smapfile=test.smap", "html=test", "sources=s1,s2,s3"});
    Assert.assertNotNull(args.getDataFile());
    Assert.assertNotNull(args.getSourceMapFile());
    Assert.assertNotNull(args.getHtmlDir());
    Assert.assertEquals("test", args.getHtmlDir().getName());
    final List<File> sources = args.getSourceDirs();
    Assert.assertNotNull(sources);
    Assert.assertEquals(3, sources.size());
    for (File f : sources) {
      Assert.assertTrue(f.getName().startsWith("s"));
    }
  }

  @Test
  public void testQuotes() throws Exception {
    final ReporterArgs args = ReporterArgs.from(new String[]{"datafile=\"test.ic\"", "smapfile=\"test.smap\"", "html=\"test\"", "sources=\"s1\",\"s2\",\"s3\""});
    Assert.assertNotNull(args.getDataFile());
    Assert.assertEquals("test.ic", args.getDataFile().getName());
    Assert.assertNotNull(args.getSourceMapFile());
    Assert.assertEquals("test.smap", args.getSourceMapFile().getName());
    Assert.assertNotNull(args.getHtmlDir());
    Assert.assertEquals("test", args.getHtmlDir().getName());
    final List<File> sources = args.getSourceDirs();
    Assert.assertNotNull(sources);
    Assert.assertEquals(3, sources.size());
    for (File f : sources) {
      Assert.assertTrue(f.getName().startsWith("s"));
    }
  }

  @Test
  public void testHelp() {
    Assert.assertNotNull(ReporterArgs.getHelpString());
  }
}
