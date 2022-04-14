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

package com.intellij.rt.coverage.report;

import com.intellij.rt.coverage.aggregate.Aggregator;
import com.intellij.rt.coverage.report.util.FileUtils;
import com.intellij.rt.coverage.util.classFinder.ClassFilter;
import com.intellij.rt.coverage.verify.Verifier;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class VerifierTest {
  @Test
  public void test1() throws IOException, InterruptedException {
    final List<Verifier.Rule> rules = new ArrayList<Verifier.Rule>();
    final Verifier.Bound bound1_1 = new Verifier.Bound(1, Verifier.Counter.LINE, Verifier.ValueType.COVERED, BigDecimal.valueOf(10), BigDecimal.valueOf(15));
    final Verifier.Bound bound1_2 = new Verifier.Bound(2, Verifier.Counter.BRANCH, Verifier.ValueType.COVERED_RATE, BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.9));
    rules.add(createRule(Verifier.Target.ALL, bound1_1, bound1_2));

    final Verifier.Bound bound2_1 = new Verifier.Bound(1, Verifier.Counter.INSTRUCTION, Verifier.ValueType.COVERED, BigDecimal.valueOf(10), BigDecimal.valueOf(15));
    final Verifier.Bound bound2_2 = new Verifier.Bound(2, Verifier.Counter.BRANCH, Verifier.ValueType.MISSED_RATE, BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.9));
    rules.add(createRule(Verifier.Target.CLASS, bound2_1, bound2_2));

    final Verifier.Bound bound3_1 = new Verifier.Bound(1, Verifier.Counter.LINE, Verifier.ValueType.MISSED, BigDecimal.valueOf(10), BigDecimal.valueOf(15));
    final Verifier.Bound bound3_2 = new Verifier.Bound(2, Verifier.Counter.BRANCH, Verifier.ValueType.COVERED_RATE, BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.9));
    rules.add(createRule(Verifier.Target.PACKAGE, bound3_1, bound3_2));

    runVerifier(rules, "verifier_test1.json");
  }

  @Test
  public void test2() throws IOException, InterruptedException {
    final List<Verifier.Rule> rules = new ArrayList<Verifier.Rule>();
    final Verifier.Bound bound1_1 = new Verifier.Bound(1, Verifier.Counter.LINE, Verifier.ValueType.COVERED, BigDecimal.valueOf(10), BigDecimal.valueOf(15));
    final Verifier.Bound bound1_2 = new Verifier.Bound(2, Verifier.Counter.BRANCH, Verifier.ValueType.COVERED_RATE, BigDecimal.valueOf(0.0), null);
    rules.add(createRule(Verifier.Target.ALL, bound1_1, bound1_2));

    final Verifier.Bound bound2_1 = new Verifier.Bound(1, Verifier.Counter.INSTRUCTION, Verifier.ValueType.COVERED, BigDecimal.valueOf(5), BigDecimal.valueOf(15));
    final Verifier.Bound bound2_2 = new Verifier.Bound(2, Verifier.Counter.BRANCH, Verifier.ValueType.MISSED_RATE, BigDecimal.valueOf(0.0), null);
    rules.add(createRule(Verifier.Target.ALL, bound2_1, bound2_2));

    final Verifier.Bound bound3_1 = new Verifier.Bound(1, Verifier.Counter.LINE, Verifier.ValueType.MISSED, BigDecimal.valueOf(10), BigDecimal.valueOf(70));
    final Verifier.Bound bound3_2 = new Verifier.Bound(2, Verifier.Counter.INSTRUCTION, Verifier.ValueType.COVERED_RATE, BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.9));
    rules.add(createRule(Verifier.Target.ALL, bound3_1, bound3_2));

    runVerifier(rules, "verifier_test2.json");
  }

  private File getFile() throws IOException {
    return File.createTempFile("report", "ic");
  }

  private int ruleId = 1;

  private Verifier.Rule createRule(Verifier.Target target, Verifier.Bound... bounds) throws IOException {
    return new Verifier.Rule(ruleId++, getFile(), target, Arrays.asList(bounds));
  }

  public static void runVerifier(List<Verifier.Rule> rules, String expectedFileName) throws IOException, InterruptedException {
    final List<Aggregator.Request> requests = new ArrayList<Aggregator.Request>();
    for (Verifier.Rule rule : rules) {
      final Aggregator.Request request = new Aggregator.Request(
          new ClassFilter.PatternFilter(
              Collections.singletonList(Pattern.compile("testData\\..*")),
              Collections.<Pattern>emptyList()),
          rule.reportFile);
      requests.add(request);
    }
    AggregatorTest.runAggregator(requests, "testData.branches.TestKt", "testData.inline.Test", "testData.simple.Main");

    final Verifier verifier = new Verifier(rules);
    final File outputFile = File.createTempFile("result", "json");
    verifier.processRules(outputFile);
    final File expected = TestUtils.getResourceFile(expectedFileName);
    Assert.assertEquals(FileUtils.readAll(expected), FileUtils.readAll(outputFile));
  }
}
