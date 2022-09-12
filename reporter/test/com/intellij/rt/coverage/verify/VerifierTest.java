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

package com.intellij.rt.coverage.verify;

import com.intellij.rt.coverage.aggregate.Aggregator;
import com.intellij.rt.coverage.aggregate.AggregatorTest;
import com.intellij.rt.coverage.report.TestUtils;
import com.intellij.rt.coverage.report.data.Filters;
import com.intellij.rt.coverage.report.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
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

    runVerifier(rules, "verify/test1.json");
  }

  @Test
  public void test2() throws IOException, InterruptedException {
    final List<Verifier.Rule> rules = new ArrayList<Verifier.Rule>();
    final Verifier.Bound bound1_1 = new Verifier.Bound(1, Verifier.Counter.LINE, Verifier.ValueType.COVERED, BigDecimal.valueOf(10), BigDecimal.valueOf(15));
    final Verifier.Bound bound1_2 = new Verifier.Bound(2, Verifier.Counter.BRANCH, Verifier.ValueType.COVERED_RATE, BigDecimal.valueOf(0.0), null);
    rules.add(createRule(Verifier.Target.ALL, bound1_1, bound1_2));

    final Verifier.Bound bound2_1 = new Verifier.Bound(1, Verifier.Counter.INSTRUCTION, Verifier.ValueType.COVERED, BigDecimal.valueOf(5), BigDecimal.valueOf(43));
    final Verifier.Bound bound2_2 = new Verifier.Bound(2, Verifier.Counter.BRANCH, Verifier.ValueType.MISSED_RATE, BigDecimal.valueOf(0.0), null);
    rules.add(createRule(Verifier.Target.ALL, bound2_1, bound2_2));

    final Verifier.Bound bound3_1 = new Verifier.Bound(1, Verifier.Counter.LINE, Verifier.ValueType.MISSED, BigDecimal.valueOf(10), BigDecimal.valueOf(70));
    final Verifier.Bound bound3_2 = new Verifier.Bound(2, Verifier.Counter.INSTRUCTION, Verifier.ValueType.COVERED_RATE, BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.9));
    rules.add(createRule(Verifier.Target.ALL, bound3_1, bound3_2));

    runVerifier(rules, "verify/test2.json");
  }

  @Test
  public void test3() throws IOException, InterruptedException {
    final List<Verifier.Rule> rules = new ArrayList<Verifier.Rule>();
    final Verifier.Bound bound1_1 = new Verifier.Bound(1, Verifier.Counter.LINE, Verifier.ValueType.COVERED, BigDecimal.valueOf(10), BigDecimal.valueOf(15));
    rules.add(createRule(Verifier.Target.PACKAGE, bound1_1));

    runVerifier(rules, "verify/test3.json");
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
    final List<Pattern> includes = new ArrayList<Pattern>();
    includes.add(Pattern.compile("testData\\.branches\\..*"));
    includes.add(Pattern.compile("testData\\.crossinline\\..*"));
    includes.add(Pattern.compile("testData\\.defaultArgs\\..*"));
    includes.add(Pattern.compile("testData\\.inline\\..*"));
    includes.add(Pattern.compile("testData\\.noReport\\..*"));
    includes.add(Pattern.compile("testData\\.simple\\..*"));
    includes.add(Pattern.compile("testData\\.outOfPackageStructure\\..*"));
    includes.add(Pattern.compile("[^.]*"));
    for (Verifier.Rule rule : rules) {
      final Aggregator.Request request = new Aggregator.Request(
          new Filters(includes, Collections.<Pattern>emptyList(), Collections.<Pattern>emptyList()),
          rule.reportFile, null);
      requests.add(request);
    }
    AggregatorTest.runAggregator(requests, "testData.branches.TestKt", "testData.inline.TestKt", "testData.simple.Main", "TestTopLevelKt");

    final Verifier verifier = new Verifier(rules);
    check(expectedFileName, verifier);
  }

  private static void check(String expectedFileName, Verifier verifier) throws IOException {
    final File outputFile = File.createTempFile("result", "json");
    verifier.processRules(outputFile);
    final File expected = TestUtils.getResourceFile(expectedFileName);

    final String expectedString = FileUtils.readAll(expected);
    final String actualString = FileUtils.readAll(outputFile);
//    Assert.assertEquals(expectedString, actualString);

    // compare as a set of strings to avoid  errors with order
    final Set<String> expectedSet = new HashSet<String>(Arrays.asList(expectedString.split("\n")));
    final Set<String> actualSet = new HashSet<String>(Arrays.asList(actualString.split("\n")));
    Assert.assertEquals(expectedSet, actualSet);
  }
}
