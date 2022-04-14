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

import com.intellij.rt.coverage.report.ArgParseException;
import com.intellij.rt.coverage.report.ReporterArgs;
import com.intellij.rt.coverage.report.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class VerifierArgs {
  static final String RESULT_FILE_TAG = "resultFile";
  static final String AGGREGATED_FILE_TAG = "aggregatedReportFile";
  static final String RULES_TAG = "rules";
  static final String ID_TAG = "id";
  static final String TARGET_TAG = "targetType";
  static final String BOUNDS_TAG = "bounds";
  static final String COUNTER_TAG = "counter";
  static final String VALUE_TAG = "valueType";
  static final String MIN_TAG = "min";
  static final String MAX_TAG = "max";

  public final File resultFile;
  public final List<Verifier.Rule> rules;

  public VerifierArgs(File resultFile, List<Verifier.Rule> rules) {
    this.resultFile = resultFile;
    this.rules = rules;
  }

  public static VerifierArgs from(String[] args) throws ArgParseException {
    final File argsFile = ReporterArgs.getArgsFile(args);
    try {
      return parse(argsFile);
    } catch (IOException e) {
      throw new ArgParseException(e);
    } catch (JSONException e) {
      throw new ArgParseException("Incorrect arguments in file " + argsFile.getAbsolutePath(), e);
    }
  }

  public static VerifierArgs parse(File argsFile) throws IOException {
    final String jsonString = FileUtils.readAll(argsFile);
    final JSONObject args = new JSONObject(jsonString);

    final File resultFile = new File(args.getString(RESULT_FILE_TAG));
    final List<Verifier.Rule> ruleList = parseRules(args);
    return new VerifierArgs(resultFile, ruleList);
  }

  private static List<Verifier.Rule> parseRules(JSONObject args) {
    final List<Verifier.Rule> ruleList = new ArrayList<Verifier.Rule>();
    final JSONArray rules = args.getJSONArray(RULES_TAG);
    for (int i = 0; i < rules.length(); i++) {
      final JSONObject rule = rules.getJSONObject(i);

      final int id = rule.getInt(ID_TAG);
      final File reportFile = new File(rule.getString(AGGREGATED_FILE_TAG));
      final Verifier.Target target = Verifier.Target.valueOf(rule.getString(TARGET_TAG));
      final List<Verifier.Bound> boundList = parseBounds(rule);

      if (boundList.isEmpty()) continue;

      ruleList.add(new Verifier.Rule(id, reportFile, target, boundList));
    }
    return ruleList;
  }

  private static List<Verifier.Bound> parseBounds(JSONObject args) {
    final List<Verifier.Bound> boundList = new ArrayList<Verifier.Bound>();
    final JSONArray bounds = args.getJSONArray(BOUNDS_TAG);
    for (int i = 0; i < bounds.length(); i++) {
      final JSONObject bound = bounds.getJSONObject(i);

      final int id = bound.getInt(ID_TAG);
      final Verifier.Counter counter = Verifier.Counter.valueOf(bound.getString(COUNTER_TAG));
      final Verifier.ValueType valueType = Verifier.ValueType.valueOf(bound.getString(VALUE_TAG));
      final BigDecimal min = bound.has(MIN_TAG) ? bound.getBigDecimal(MIN_TAG) : null;
      final BigDecimal max = bound.has(MAX_TAG) ? bound.getBigDecimal(MAX_TAG) : null;
      if (min == null && max == null) continue;
      boundList.add(new Verifier.Bound(id, counter, valueType, min, max));
    }
    return boundList;
  }

  public static String getHelpString() {
    return "Arguments must be passed in the following JSON format:\n" +
        "{\n" +
        "  \"resultFile\": String,\n" +
        "\t\"rules\": [{\n" +
        "\t  \"id\": Int,\n" +
        "\t  \"aggregatedReportFile\": String,\n" +
        "\t  \"targetType\": String, // enum values: \"CLASS\", \"PACKAGE\", \"ALL\"\n" +
        "\t  \"bounds\": [ \n" +
        "\t    {\n" +
        "\t      \"id\": Int,\n" +
        "\t      \"counter\": String, // \"LINE\", \"INSTRUCTION\", \"BRANCH\"\n" +
        "\t      \"valueType\": String, // \"MISSED\", \"COVERED\", \"MISSED_RATE\", \"COVERED_RATE\"\n" +
        "\t      \"min\": BigDecimal, // optional\n" +
        "          \"max\": BigDecimal, // optional\n" +
        "\t    },...\n" +
        "\t  ]\n" +
        "\t},...\n" +
        "\t]\n" +
        "}";
  }
}
