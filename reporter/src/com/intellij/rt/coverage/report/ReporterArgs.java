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
import com.intellij.rt.coverage.report.data.Module;
import com.intellij.rt.coverage.report.util.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReporterArgs {
  static final String XML_FILE_TAG = "xml";
  static final String HTML_DIR_TAG = "html";
  static final String MODULES_TAG = "modules";
  static final String REPORTS_TAG = "reports";
  static final String IC_FILE_TAG = "ic";
  static final String SMAP_FILE_TAG = "smap";
  static final String OUTPUTS_TAG = "output";
  static final String SOURCES_TAG = "sources";


  public final List<Module> modules;
  public final File xmlFile;
  public final File htmlDir;

  ReporterArgs(List<Module> modules, File xmlFile, File htmlDir) {
    this.modules = modules;
    this.xmlFile = xmlFile;
    this.htmlDir = htmlDir;
  }

  public static ReporterArgs from(String[] args) throws ArgParseException {
    if (args.length != 1) {
      throw new ArgParseException("Single file name argument expected, but " + args.length + " arguments found.");
    }
    final String fileName = args[0];
    final File argsFile = new File(fileName);
    if (!(argsFile.exists() && argsFile.isFile())) {
      throw new ArgParseException("Expected file with arguments, but " + fileName + " is not a file.");
    }
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

    final List<Module> moduleList = new ArrayList<Module>();
    final JSONArray modules = args.getJSONArray(MODULES_TAG);
    for (int moduleId = 0; moduleId < modules.length(); moduleId++) {
      final JSONObject module = modules.getJSONObject(moduleId);

      final List<BinaryReport> reportList = new ArrayList<BinaryReport>();
      if (module.has(REPORTS_TAG)) {
        final JSONArray reports = module.getJSONArray(REPORTS_TAG);
        for (int reportId = 0; reportId < reports.length(); reportId++) {
          final JSONObject report = reports.getJSONObject(reportId);
          final String icPath = report.getString(IC_FILE_TAG);
          final String smapPath = report.getString(SMAP_FILE_TAG);
          reportList.add(new BinaryReport(new File(icPath), new File(smapPath)));
        }
      }

      moduleList.add(new Module(reportList, parsePathList(module, OUTPUTS_TAG), parsePathList(module, SOURCES_TAG)));
    }

    final File xmlFile = args.has(XML_FILE_TAG) ? new File(args.getString(XML_FILE_TAG)) : null;
    final File htmlDir = args.has(HTML_DIR_TAG) ? new File(args.getString(HTML_DIR_TAG)) : null;

    return new ReporterArgs(moduleList, xmlFile, htmlDir);
  }

  private static List<File> parsePathList(JSONObject module, String tag) {
    if (!module.has(tag)) return null;
    final JSONArray array = module.getJSONArray(tag);
    final List<File> result = new ArrayList<File>();
    for (int i = 0; i < array.length(); i++) {
      final String path = array.getString(i);
      result.add(new File(path));
    }
    return result;
  }

  public static String getHelpString() {
    return "Arguments must be passed in the following JSON format:\n" +
        "{\n" +
        "  \"modules\": [\n" +
        "    { \"reports\": [\n" +
        "        {\"ic\": \"path to ic binary file\", \"smap\": \"path to source map file\"}\n" +
        "      ] [OPTIONAL, absence means that all classes were not covered],\n" +
        "      \"output\": [\"outputRoot1\", \"outputRoot2\"],\n" +
        "      \"sources\": [\"sourceRoot1\", \"sourceRoot2\"]\n" +
        "    }\n" +
        "  ],\n" +
        "  \"xml\": \"path to xml file\" [OPTIONAL],\n" +
        "  \"html\": \"path to html directory\" [OPTIONAL]\n" +
        "}";
  }

  public static class ArgParseException extends Exception {
    ArgParseException(String message) {
      super(message);
    }

    ArgParseException(Throwable cause) {
      super(cause);
    }

    ArgParseException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
