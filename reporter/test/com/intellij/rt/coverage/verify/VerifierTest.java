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

import com.intellij.rt.coverage.aggregate.AggregatorTest;
import com.intellij.rt.coverage.aggregate.api.Request;
import com.intellij.rt.coverage.report.TestUtils;
import com.intellij.rt.coverage.verify.api.*;
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
    List<Rule> rules = new ArrayList<Rule>();
    Bound bound1_1 = new Bound(1, Counter.LINE, ValueType.COVERED, BigDecimal.valueOf(10), BigDecimal.valueOf(15));
    Bound bound1_2 = new Bound(2, Counter.BRANCH, ValueType.COVERED_RATE, BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.9));
    rules.add(createRule(Target.ALL, bound1_1, bound1_2));

    Bound bound2_1 = new Bound(1, Counter.INSTRUCTION, ValueType.COVERED, BigDecimal.valueOf(10), BigDecimal.valueOf(15));
    Bound bound2_2 = new Bound(2, Counter.BRANCH, ValueType.MISSED_RATE, BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.9));
    rules.add(createRule(Target.CLASS, bound2_1, bound2_2));

    Bound bound3_1 = new Bound(1, Counter.LINE, ValueType.MISSED, BigDecimal.valueOf(10), BigDecimal.valueOf(15));
    Bound bound3_2 = new Bound(2, Counter.BRANCH, ValueType.COVERED_RATE, BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.9));
    rules.add(createRule(Target.PACKAGE, bound3_1, bound3_2));


    BoundViolation boundViolation1x2 = new BoundViolation(2);
    boundViolation1x2.minViolations.add(new Violation("all", new BigDecimal("0.090909")));
    RuleViolation rule1 = new RuleViolation(1, Collections.singletonList(boundViolation1x2));

    BoundViolation boundViolation2x1 = new BoundViolation(1);
    boundViolation2x1.minViolations.add(new Violation("testData.noReport.branches.MyBranchedUnloadedClass", new BigDecimal("0")));
    boundViolation2x1.minViolations.add(new Violation("TestTopLevelKt", new BigDecimal("2")));
    boundViolation2x1.minViolations.add(new Violation("testData.branches.MyBranchedClass", new BigDecimal("9")));
    boundViolation2x1.minViolations.add(new Violation("testData.branches.MyBranchedUnloadedClass", new BigDecimal("0")));
    boundViolation2x1.minViolations.add(new Violation("testData.noReport.branches.MyBranchedClass", new BigDecimal("0")));
    boundViolation2x1.minViolations.add(new Violation("testData.crossinline.TestKt$main$$inlined$run$1", new BigDecimal("0")));
    boundViolation2x1.minViolations.add(new Violation("testData.crossinline.TestKt$run$2", new BigDecimal("0")));
    boundViolation2x1.minViolations.add(new Violation("testData.noReport.branches.TestKt", new BigDecimal("0")));
    boundViolation2x1.minViolations.add(new Violation("testData.defaultArgs.Example", new BigDecimal("0")));
    boundViolation2x1.minViolations.add(new Violation("testData.crossinline.TestKt$main$3", new BigDecimal("0")));
    boundViolation2x1.minViolations.add(new Violation("testData.defaultArgs.TestKt", new BigDecimal("0")));
    boundViolation2x1.minViolations.add(new Violation("testData.outOfPackageStructure.TestOutOfPackageStructureKt", new BigDecimal("0")));
    boundViolation2x1.minViolations.add(new Violation("testData.branches.TestKt", new BigDecimal("5")));
    boundViolation2x1.minViolations.add(new Violation("testData.crossinline.TestKt", new BigDecimal("0")));

    boundViolation2x1.maxViolations.add(new Violation("testData.simple.Main", new BigDecimal("16")));
    boundViolation2x1.maxViolations.add(new Violation("testData.inline.TestKt", new BigDecimal("18")));

    BoundViolation boundViolation2x2 = new BoundViolation(2);
    boundViolation2x2.maxViolations.add(new Violation("testData.noReport.branches.MyBranchedUnloadedClass", new BigDecimal("1.000000")));
    boundViolation2x2.maxViolations.add(new Violation("testData.branches.MyBranchedUnloadedClass", new BigDecimal("1.000000")));
    boundViolation2x2.maxViolations.add(new Violation("testData.noReport.branches.MyBranchedClass", new BigDecimal("1.000000")));

    RuleViolation rule2 = new RuleViolation(2, Arrays.asList(boundViolation2x1, boundViolation2x2));

    BoundViolation boundViolation3x1 = new BoundViolation(1);
    boundViolation3x1.minViolations.add(new Violation("", BigDecimal.ZERO));
    boundViolation3x1.minViolations.add(new Violation("testData.inline", new BigDecimal("2")));
    boundViolation3x1.minViolations.add(new Violation("testData.defaultArgs", new BigDecimal("6")));
    boundViolation3x1.minViolations.add(new Violation("testData.outOfPackageStructure", new BigDecimal("1")));
    boundViolation3x1.minViolations.add(new Violation("testData.simple", new BigDecimal("4")));
    boundViolation3x1.minViolations.add(new Violation("testData.crossinline", new BigDecimal("4")));

    boundViolation3x1.maxViolations.add(new Violation("testData.noReport.branches", new BigDecimal("23")));
    boundViolation3x1.maxViolations.add(new Violation("testData.branches", new BigDecimal("19")));

    BoundViolation boundViolation3x2 = new BoundViolation(2);
    boundViolation3x2.minViolations.add(new Violation("testData.noReport.branches", new BigDecimal("0.000000")));
    boundViolation3x2.minViolations.add(new Violation("testData.branches", new BigDecimal("0.071429")));

    RuleViolation rule3 = new RuleViolation(3, Arrays.asList(boundViolation3x1, boundViolation3x2));

    runVerifier(rules, Arrays.asList(rule1, rule2, rule3));
  }

  @Test
  public void test2() throws IOException, InterruptedException {
    List<Rule> rules = new ArrayList<Rule>();
    Bound bound1_1 = new Bound(1, Counter.LINE, ValueType.COVERED, BigDecimal.valueOf(10), BigDecimal.valueOf(15));
    Bound bound1_2 = new Bound(2, Counter.BRANCH, ValueType.COVERED_RATE, BigDecimal.valueOf(0.0), null);
    rules.add(createRule(Target.ALL, bound1_1, bound1_2));

    Bound bound2_1 = new Bound(1, Counter.INSTRUCTION, ValueType.COVERED, BigDecimal.valueOf(5), BigDecimal.valueOf(52));
    Bound bound2_2 = new Bound(2, Counter.BRANCH, ValueType.MISSED_RATE, BigDecimal.valueOf(0.0), null);
    rules.add(createRule(Target.ALL, bound2_1, bound2_2));

    Bound bound3_1 = new Bound(1, Counter.LINE, ValueType.MISSED, BigDecimal.valueOf(10), BigDecimal.valueOf(70));
    Bound bound3_2 = new Bound(2, Counter.INSTRUCTION, ValueType.COVERED_RATE, BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.9));
    rules.add(createRule(Target.ALL, bound3_1, bound3_2));

    runVerifier(rules, Collections.<RuleViolation>emptyList());
  }

  @Test
  public void test3() throws IOException, InterruptedException {
    List<Rule> rules = new ArrayList<Rule>();
    Bound bound1_1 = new Bound(1, Counter.LINE, ValueType.COVERED, BigDecimal.valueOf(10), BigDecimal.valueOf(15));
    rules.add(createRule(Target.PACKAGE, bound1_1));

    BoundViolation boundViolation = new BoundViolation(1);
    boundViolation.minViolations.add(new Violation("", BigDecimal.ONE));
    boundViolation.minViolations.add(new Violation("testData.inline", new BigDecimal("3")));
    boundViolation.minViolations.add(new Violation("testData.defaultArgs", BigDecimal.ZERO));
    boundViolation.minViolations.add(new Violation("testData.outOfPackageStructure", BigDecimal.ZERO));
    boundViolation.minViolations.add(new Violation("testData.simple", new BigDecimal("5")));
    boundViolation.minViolations.add(new Violation("testData.crossinline", BigDecimal.ZERO));
    boundViolation.minViolations.add(new Violation("testData.noReport.branches", BigDecimal.ZERO));
    boundViolation.minViolations.add(new Violation("testData.branches", new BigDecimal("4")));

    RuleViolation ruleViolation = new RuleViolation(1, Collections.singletonList(boundViolation));
    runVerifier(rules, Collections.singletonList(ruleViolation));
  }

  private File getFile() throws IOException {
    return File.createTempFile("report", "ic");
  }

  private int ruleId = 1;

  private Rule createRule(Target target, Bound... bounds) throws IOException {
    return new Rule(ruleId++, getFile(), target, Arrays.asList(bounds));
  }

  public static void runVerifier(List<Rule> rules, List<RuleViolation> expected) throws IOException, InterruptedException {
    List<Request> requests = new ArrayList<Request>();
    List<Pattern> includes = new ArrayList<Pattern>();
    includes.add(Pattern.compile("testData\\.branches\\..*"));
    includes.add(Pattern.compile("testData\\.crossinline\\..*"));
    includes.add(Pattern.compile("testData\\.defaultArgs\\..*"));
    includes.add(Pattern.compile("testData\\.inline\\..*"));
    includes.add(Pattern.compile("testData\\.noReport\\..*"));
    includes.add(Pattern.compile("testData\\.simple\\..*"));
    includes.add(Pattern.compile("testData\\.outOfPackageStructure\\..*"));
    includes.add(Pattern.compile("[^.]*"));
    for (Rule rule : rules) {
      Request request = new Request(
          TestUtils.createFilters(includes),
          rule.reportFile, null);
      requests.add(request);
    }
    TestUtils.clearLogFile(new File("."));
    AggregatorTest.runAggregator(requests, "testData.branches.TestKt", "testData.inline.TestKt", "testData.simple.Main", "TestTopLevelKt");
    TestUtils.checkLogFile(new File("."));


    TestUtils.clearLogFile(new File("."));
    List<RuleViolation> actual = VerificationApi.verify(rules);
    TestUtils.checkLogFile(new File("."));
    check(expected, actual);
  }

  private static void check(List<RuleViolation> expected, List<RuleViolation> actual) {
    Assert.assertEquals(expected.size(), actual.size());

    for (int i = 0; i < expected.size(); i++) {
      RuleViolation expectedRule = expected.get(i);
      RuleViolation actualRule = actual.get(i);
      Assert.assertEquals(expectedRule.id, actualRule.id);

      Assert.assertEquals(expectedRule.violations.size(), actualRule.violations.size());
      for (int j = 0; j < expectedRule.violations.size(); j++) {
        BoundViolation expectedBound = expectedRule.violations.get(j);
        BoundViolation actualBound = actualRule.violations.get(j);
        Assert.assertEquals(expectedBound.id, actualBound.id);

        checkViolations(expectedBound.maxViolations, actualBound.maxViolations);
        checkViolations(expectedBound.minViolations, actualBound.minViolations);
      }
    }
  }

  private static void checkViolations(List<Violation> expected, List<Violation> actual) {
    Assert.assertEquals(expected.size(), actual.size());

    for (int i = 0; i < expected.size(); i++) {
      Violation expectedV = expected.get(i);
      Violation actualV = actual.get(i);

      Assert.assertEquals(expectedV.targetName, actualV.targetName);
      Assert.assertEquals(expectedV.targetValue, actualV.targetValue);
    }
  }
}
