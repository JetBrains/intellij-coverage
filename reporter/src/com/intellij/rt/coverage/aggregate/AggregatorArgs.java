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

package com.intellij.rt.coverage.aggregate;

import com.intellij.rt.coverage.report.ArgParseException;
import com.intellij.rt.coverage.report.ReporterArgs;
import com.intellij.rt.coverage.report.data.BinaryReport;
import com.intellij.rt.coverage.report.data.Filters;
import com.intellij.rt.coverage.report.data.Module;
import com.intellij.rt.coverage.report.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AggregatorArgs {
  static final String RESULTS_TAG = "result";
  static final String AGGREGATED_FILE_TAG = "aggregatedReportFile";
  static final String FILTERS_TAG = "filters";

  public final List<BinaryReport> reports;
  public final List<Module> modules;
  public final List<Aggregator.Request> requests;

  public AggregatorArgs(List<BinaryReport> reports, List<Module> modules, List<Aggregator.Request> requests) {
    this.reports = reports;
    this.modules = modules;
    this.requests = requests;
  }

  public static AggregatorArgs from(String[] args) throws ArgParseException {
    final File argsFile = ReporterArgs.getArgsFile(args);
    try {
      return parse(argsFile);
    } catch (IOException e) {
      throw new ArgParseException(e);
    } catch (JSONException e) {
      throw new ArgParseException("Incorrect arguments in file " + argsFile.getAbsolutePath(), e);
    }
  }

  public static AggregatorArgs parse(File argsFile) throws IOException {
    final String jsonString = FileUtils.readAll(argsFile);
    final JSONObject args = new JSONObject(jsonString);

    final List<Module> moduleList = ReporterArgs.parseModules(args);
    final List<BinaryReport> reportList = ReporterArgs.parseReports(args);

    final List<Aggregator.Request> requestList = new ArrayList<Aggregator.Request>();
    final JSONArray requests = args.getJSONArray(RESULTS_TAG);
    for (int requestId = 0; requestId < requests.length(); requestId++) {
      final JSONObject request = requests.getJSONObject(requestId);
      final File outputFile = new File(request.getString(AGGREGATED_FILE_TAG));
      Filters filters = Filters.EMPTY;
      if (request.has(FILTERS_TAG)) {
        final JSONObject filterArgs = request.getJSONObject(FILTERS_TAG);
        filters = ReporterArgs.parseFilters(filterArgs);
      }
      requestList.add(new Aggregator.Request(filters, outputFile));
    }

    return new AggregatorArgs(reportList, moduleList, requestList);
  }

  public static String getHelpString() {
    return "Arguments must be passed in the following JSON format:\n" +
        "{\n" +
        "  reports: [{ic: \"path\", smap: \"path\" [OPTIONAL]}, ...],\n" +
        "  modules: [{output: [\"path1\", \"path2\"], sources: [\"source1\", ...]}, {...}],\n" +
        "  result: [{\n" +
        "    aggregatedReportFile: \"path\",\n" +
        "    filters: { [OPTIONAL]\n" +
        "      include: {\n" +
        "        classes: [\"regex1\", \"regex2\"] [OPTIONAL]\n" +
        "      } [OPTIONAL],\n" +
        "      exclude: {\n" +
        "        classes: [\"regex1\", \"regex2\"], [OPTIONAL]\n" +
        "        annotations: [\"regex1\", \"regex2\"] [OPTIONAL]\n" +
        "      } [OPTIONAL],\n" +
        "    }\n" +
        "  }, ...]\n" +
        "}";
  }
}
