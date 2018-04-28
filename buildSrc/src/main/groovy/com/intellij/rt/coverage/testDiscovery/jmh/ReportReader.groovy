/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package com.intellij.rt.coverage.testDiscovery.jmh

import groovy.json.JsonSlurper

import java.math.RoundingMode

final class ReportReader {
  static def readScore(String baseReport, String nextReport, List<String> secondaryMetrics = []) {
    def (base, next) = [
        parse(baseReport, secondaryMetrics),
        parse(nextReport, secondaryMetrics)
    ]
    if (base.keySet() != next.keySet()) {
      throw new IllegalArgumentException("${base.keySet()} != ${next.keySet()}")
    }
    next.keySet().collect { benchmark ->
      "$benchmark: ${compare base[benchmark], next[benchmark]}"
    }.join('\n\t\t')
  }

  private static def compare(Report base, Report next) {
    if (base.mode != next.mode || base.scoreUnit != next.scoreUnit) {
      throw new IllegalArgumentException("${base.mode} != ${next.mode} || ${base.scoreUnit} != ${next.scoreUnit}")
    }
    def isGrowthPositive = next.mode == 'thrpt'
    def score = percentage(next.score.subtract(base.score), base.score)
    def isPositive = isGrowthPositive && score >= 0 ||
        !isGrowthPositive && score <= 0
    def error = percentage(base.scoreError, base.score)
        .add(percentage(next.scoreError, next.score))
    "${base.mode != null ? "[$base.mode] " : ''}${score.abs()}% ${isPositive ? 'speedup' : 'degradation'} ± $error%"
  }

  private static def percentage(BigDecimal x, BigDecimal y) {
    if (BigDecimal.ZERO != y) {
      (x.divide(y, 2, RoundingMode.HALF_DOWN) * 100)
          .setScale(0, RoundingMode.HALF_DOWN)
    } else {
      BigDecimal.ZERO
    }
  }

  private static Map<String, Report> parse(String jsonReport, List<String> secondaryMetrics) {
    def result = new LinkedHashMap<String, Report>(++secondaryMetrics.size())
    (new JsonSlurper().parseText(jsonReport) as List).each { json ->
      def benchmark = json.benchmark.toString().split(/\./).last()
      result[benchmark] = read json.primaryMetric, json.mode
      secondaryMetrics.each {
        json.secondaryMetrics?."$it"?.with { sm ->
          result[it] = read sm
        }
      }
    }
    result
  }

  private static def read(json, mode = null) {
    new Report(
        mode: mode,
        scoreUnit: json.scoreUnit,
        score: json.score.toBigDecimal(),
        scoreError: json.scoreError.with {
          it == 'NaN' ? BigDecimal.ZERO : it.toBigDecimal()
        }
    )
  }

  private static class Report {
    String mode, scoreUnit
    BigDecimal score, scoreError
  }
}