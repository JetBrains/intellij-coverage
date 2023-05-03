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
import com.intellij.rt.coverage.util.ProjectDataLoader;
import com.intellij.rt.coverage.verify.api.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sums up coverage statistics and checks if user defined coverage restrictions are passing
 */
public class Verifier {
  private final List<Rule> myRules;

  public Verifier(List<Rule> rules) {
    myRules = rules;
  }

  /**
   * Check all rules and save a report on failed rules.
   *
   * @return violations
   */
  public List<RuleViolation> processRules() {
    final List<RuleViolation> violations = new ArrayList<RuleViolation>();
    for (Rule rule : myRules) {
      final RuleViolation violation = processRule(rule);
      if (violation == null) continue;
      violations.add(violation);
    }
    return violations;
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

  /**
   * Line, instructions, and branch statistics.
   */
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
