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

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.report.util.FileUtils;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Verifier {
  private final List<Rule> myRules;

  public Verifier(List<Rule> rules) {
    myRules = rules;
  }


  public void processRules(File outputFile) throws IOException {
    final List<RuleViolation> violations = new ArrayList<RuleViolation>();
    for (Rule rule : myRules) {
      final RuleViolation violation = processRule(rule);
      if (violation == null) continue;
      violations.add(violation);
    }
    saveViolations(violations, outputFile);
  }

  private static RuleViolation processRule(final Rule rule) {
    final ProjectData projectData = ProjectDataLoader.load(rule.reportFile);
    final Map<Integer, BoundViolation> violations = new HashMap<Integer, BoundViolation>();
    final TargetProcessor processor = rule.target.createTargetProcessor();
    processor.process(projectData, new TargetProcessor.Consumer() {

      private BoundViolation getOrCreateViolation(int boundId) {
        BoundViolation violation = violations.get(boundId);
        if (violation == null) {
          violation = new BoundViolation(boundId);
          violations.put(boundId, violation);
        }
        return violation;
      }

      @Override
      public void consume(String name, CollectedCoverage coverage) {
        for (Bound bound : rule.bounds) {
          final BigDecimal value = bound.valueType.getValue(bound.counter.getCounter(coverage));
          if (value == null) continue;
          if (bound.min != null && value.compareTo(bound.min) < 0) {
            final BoundViolation violation = getOrCreateViolation(bound.id);
            violation.minViolations.add(new Violation(name, value));
          }

          if (bound.max != null && value.compareTo(bound.max) > 0) {
            final BoundViolation violation = getOrCreateViolation(bound.id);
            violation.maxViolations.add(new Violation(name, value));
          }
        }
      }
    });

    if (violations.isEmpty()) return null;
    return new RuleViolation(rule.id, new ArrayList<BoundViolation>(violations.values()));
  }

  private static void saveViolations(List<RuleViolation> violations, File outputFile) throws IOException {
    final JSONArray jsonViolations = new JSONArray(violations.size());
    for (RuleViolation ruleViolation : violations) {
      final JSONObject jsonRuleViolation = new JSONObject();
      jsonRuleViolation.put(VerifierArgs.ID_TAG, ruleViolation.id);

      final JSONObject jsonBoundsViolation = new JSONObject();
      for (BoundViolation boundViolation : ruleViolation.violations) {
        final JSONObject jsonBoundViolation = new JSONObject();
        if (!boundViolation.minViolations.isEmpty()) {
          final JSONObject jsonMin = new JSONObject();
          for (Violation violation : boundViolation.minViolations) {
            jsonMin.put(violation.targetName, violation.targetValue);
          }
          jsonBoundViolation.put(VerifierArgs.MIN_TAG, jsonMin);
        }

        if (!boundViolation.maxViolations.isEmpty()) {
          final JSONObject jsonMax = new JSONObject();
          for (Violation violation : boundViolation.maxViolations) {
            jsonMax.put(violation.targetName, violation.targetValue);
          }
          jsonBoundViolation.put(VerifierArgs.MAX_TAG, jsonMax);
        }
        jsonBoundsViolation.put(Integer.toString(boundViolation.id), jsonBoundViolation);
      }

      jsonRuleViolation.put(VerifierArgs.BOUNDS_TAG, jsonBoundsViolation);
      jsonViolations.put(jsonRuleViolation);
    }
    FileUtils.write(outputFile, jsonViolations.toString(2));
  }

  public static class Rule {
    public final int id;
    public final File reportFile;
    public final Target target;
    public final List<Bound> bounds;


    public Rule(int id, File reportFile, Target target, List<Bound> bounds) {
      this.id = id;
      this.reportFile = reportFile;
      this.target = target;
      this.bounds = bounds;
    }
  }

  public static class Bound {
    public final int id;
    public final Counter counter;
    public final ValueType valueType;
    public final BigDecimal min;
    public final BigDecimal max;

    public Bound(int id, Counter counter, ValueType valueType, BigDecimal min, BigDecimal max) {
      this.id = id;
      this.counter = counter;
      this.valueType = valueType;
      this.min = min;
      this.max = max;
    }
  }

  public enum Target {
    CLASS,
    PACKAGE,
    ALL;

    public TargetProcessor createTargetProcessor() {
      switch (this) {
        case CLASS:
          return new ClassTargetProcessor();
        case PACKAGE:
          return new PackageTargetProcessor();
        case ALL:
          return new ProjectTargetProcessor();
      }

      throw new RuntimeException("Unexpected value " + this);
    }

  }

  public enum Counter {
    LINE,
    INSTRUCTION,
    BRANCH;

    public CollectedCoverage.Counter getCounter(CollectedCoverage coverage) {
      switch (this) {
        case LINE:
          return coverage.lineCounter;
        case INSTRUCTION:
          return coverage.instructionCounter;
        case BRANCH:
          return coverage.branchCounter;
      }
      throw new RuntimeException("Unexpected value " + this);
    }
  }

  public enum ValueType {
    MISSED,
    COVERED,
    MISSED_RATE,
    COVERED_RATE;

    public BigDecimal getValue(CollectedCoverage.Counter counter) {
      switch (this) {
        case MISSED:
          return new BigDecimal(counter.missed);
        case COVERED:
          return new BigDecimal(counter.covered);
        case MISSED_RATE: {
          final BigDecimal missed = new BigDecimal(counter.missed);
          final BigDecimal total = new BigDecimal(counter.covered + counter.missed);
          if (total.equals(BigDecimal.ZERO)) return null;
          return missed.divide(total, 6, RoundingMode.HALF_UP);
        }
        case COVERED_RATE: {
          final BigDecimal covered = new BigDecimal(counter.covered);
          final BigDecimal total = new BigDecimal(counter.covered + counter.missed);
          if (total.equals(BigDecimal.ZERO)) return null;
          return covered.divide(total, 6, RoundingMode.HALF_UP);
        }
      }
      throw new RuntimeException("Unexpected value " + this);
    }
  }

  public static class Violation {
    public final String targetName;
    public final BigDecimal targetValue;

    public Violation(String targetName, BigDecimal targetValue) {
      this.targetName = targetName;
      this.targetValue = targetValue;
    }
  }

  public static class BoundViolation {
    public final int id;
    public final List<Violation> minViolations = new ArrayList<Violation>();
    public final List<Violation> maxViolations = new ArrayList<Violation>();

    public BoundViolation(int id) {
      this.id = id;
    }
  }

  public static class RuleViolation {
    public final int id;
    public final List<BoundViolation> violations;

    public RuleViolation(int id, List<BoundViolation> violations) {
      this.id = id;
      this.violations = violations;
    }
  }

  public static class CollectedCoverage {
    public final CollectedCoverage.Counter lineCounter = new Counter();
    public final CollectedCoverage.Counter instructionCounter = new Counter();
    public final CollectedCoverage.Counter branchCounter = new Counter();

    public void add(CollectedCoverage other) {
      lineCounter.add(other.lineCounter);
      instructionCounter.add(other.instructionCounter);
      branchCounter.add(other.branchCounter);
    }

    public static class Counter {
      public long missed = 0;
      public long covered = 0;

      public void add(Counter other) {
        missed += other.missed;
        covered += other.covered;
      }

    }
  }
}
