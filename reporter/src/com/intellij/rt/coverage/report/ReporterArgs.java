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
import java.util.regex.Pattern;

public class ReporterArgs {
  static final String FORMAT_TAG = "format";
  /**
   * Default reporter format. Collects merged coverage report from scratch via aggregator.
   */
  static final String RAW_FORMAT = "raw";
  /**
   * This format assumes that a single report file is passed, which is a result of previous
   * aggregator request.
   */
  static final String AGGREGATED_BINARY_FILE_FORMAT = "kover-agg";

  static final String XML_FILE_TAG = "xml";
  static final String HTML_DIR_TAG = "html";
  public static final String TITLE_TAG = "title";
  static final String MODULES_TAG = "modules";
  static final String REPORTS_TAG = "reports";
  static final String IC_FILE_TAG = "ic";
  static final String SMAP_FILE_TAG = "smap";
  static final String OUTPUTS_TAG = "output";
  static final String SOURCES_TAG = "sources";
  public static final String INCLUDE_TAG = "include";
  public static final String EXCLUDE_TAG = "exclude";
  public static final String CLASSES_TAG = "classes";
  public static final String ANNOTATIONS_TAG = "annotations";


  public final String format;
  public final String title;
  public final List<BinaryReport> reports;
  public final List<Module> modules;
  public final File xmlFile;
  public final File htmlDir;
  public final Filters filters;

  ReporterArgs(String format, String title,
               List<BinaryReport> reportList, List<Module> modules,
               File xmlFile, File htmlDir,
               Filters filters) {
    this.format = format;
    this.title = title;
    this.reports = reportList;
    this.modules = modules;
    this.xmlFile = xmlFile;
    this.htmlDir = htmlDir;
    this.filters = filters;
  }

  public static File getArgsFile(String[] args) throws ArgParseException {
    if (args.length != 1) {
      throw new ArgParseException("Single file name argument expected, but " + args.length + " arguments found.");
    }
    final String fileName = args[0];
    final File argsFile = new File(fileName);
    if (!(argsFile.exists() && argsFile.isFile())) {
      throw new ArgParseException("Expected file with arguments, but " + fileName + " is not a file.");
    }
    return argsFile;
  }

  public static ReporterArgs from(String[] args) throws ArgParseException {
    final File argsFile = getArgsFile(args);
    try {
      return parse(argsFile);
    } catch (IOException e) {
      throw new ArgParseException(e);
    } catch (JSONException e) {
      throw new ArgParseException("Incorrect arguments in file " + argsFile.getAbsolutePath(), e);
    }
  }

  public static ReporterArgs parse(File argsFile) throws IOException, JSONException {
    final String jsonString = FileUtils.readAll(argsFile);
    final JSONObject args = new JSONObject(jsonString);

    final String format = args.has(FORMAT_TAG)
        ? args.getString(FORMAT_TAG)
        : RAW_FORMAT;
    final String title = args.has(TITLE_TAG) ? args.getString(TITLE_TAG) : "";
    final List<Module> moduleList = parseModules(args);
    final List<BinaryReport> reportList = parseReports(args);
    final Filters filters = parseFilters(args);

    final File xmlFile = args.has(XML_FILE_TAG) ? new File(args.getString(XML_FILE_TAG)) : null;
    final File htmlDir = args.has(HTML_DIR_TAG) ? new File(args.getString(HTML_DIR_TAG)) : null;

    return new ReporterArgs(format, title, reportList, moduleList, xmlFile, htmlDir, filters);
  }

  public static List<BinaryReport> parseReports(JSONObject args) {
    final List<BinaryReport> reportList = new ArrayList<BinaryReport>();
    final JSONArray reports = args.getJSONArray(REPORTS_TAG);
    for (int reportId = 0; reportId < reports.length(); reportId++) {
      final JSONObject report = reports.getJSONObject(reportId);
      final String icPath = report.getString(IC_FILE_TAG);
      final File smap = report.has(SMAP_FILE_TAG) ? new File(report.getString(SMAP_FILE_TAG)) : null;
      reportList.add(new BinaryReport(new File(icPath), smap));
    }
    return reportList;
  }

  public static List<Module> parseModules(JSONObject args) {
    final List<Module> moduleList = new ArrayList<Module>();
    final JSONArray modules = args.getJSONArray(MODULES_TAG);
    for (int moduleId = 0; moduleId < modules.length(); moduleId++) {
      final JSONObject module = modules.getJSONObject(moduleId);
      moduleList.add(new Module(parsePathList(module, OUTPUTS_TAG), parsePathList(module, SOURCES_TAG)));
    }
    return moduleList;
  }

  public static Filters parseFilters(JSONObject args) {
    final List<Pattern> includeClasses = parsePatterns(args, INCLUDE_TAG, CLASSES_TAG);
    final List<Pattern> excludeClasses = parsePatterns(args, EXCLUDE_TAG, CLASSES_TAG);
    final List<Pattern> excludeAnnotations = parsePatterns(args, EXCLUDE_TAG, ANNOTATIONS_TAG);
    return new Filters(includeClasses, excludeClasses, excludeAnnotations);
  }

  public static List<Pattern> parsePatterns(JSONObject args, String groupTag, String sectionTag) {
    final List<Pattern> patterns = new ArrayList<Pattern>();
    if (!args.has(groupTag)) return patterns;
    final JSONObject group = args.getJSONObject(groupTag);
    if (!group.has(sectionTag)) return patterns;
    final JSONArray arrayPatterns = group.getJSONArray(sectionTag);
    for (int i = 0; i < arrayPatterns.length(); i++) {
      final String pattern = arrayPatterns.getString(i);
      patterns.add(Pattern.compile(pattern));
    }
    return patterns;
  }

  private static List<File> parsePathList(JSONObject module, String tag) {
    final List<File> result = new ArrayList<File>();
    if (!module.has(tag)) return result;
    final JSONArray array = module.getJSONArray(tag);
    for (int i = 0; i < array.length(); i++) {
      final String path = array.getString(i);
      result.add(new File(path));
    }
    return result;
  }

  public static String getHelpString() {
    return "Arguments must be passed in the following JSON format:\n" +
        "{\n" +
        "  format: \"name\" [OPTIONAL],\n" +
        "  reports: [{ic: \"path\", smap: \"path\" [OPTIONAL]}, ...],\n" +
        "  modules: [{output: [\"path1\", \"path2\"], sources: [\"source1\", ...]}, {...}],\n" +
        "  xml: \"path\" [OPTIONAL],\n" +
        "  html: \"directory\" [OPTIONAL],\n" +
        "  include: {\n" +
        "        classes: [\"regex1\", \"regex2\"] [OPTIONAL]\n" +
        "   } [OPTIONAL],\n" +
        "  exclude: {\n" +
        "        classes: [\"regex1\", \"regex2\"], [OPTIONAL]\n" +
        "        annotations: [\"regex1\", \"regex2\"] [OPTIONAL]\n" +
        "   } [OPTIONAL],\n" +
        "}";
  }

}
