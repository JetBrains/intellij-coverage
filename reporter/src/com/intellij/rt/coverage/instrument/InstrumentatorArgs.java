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

package com.intellij.rt.coverage.instrument;

import com.intellij.rt.coverage.report.ArgParseException;
import com.intellij.rt.coverage.report.ReporterArgs;
import com.intellij.rt.coverage.report.data.Filters;
import com.intellij.rt.coverage.report.util.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class InstrumentatorArgs {
  static final String OUTPUT_COPY_TAG = "outputCopy";

  public final List<File> roots;
  public final List<File> outputRoots;
  public final Filters filters;

  public InstrumentatorArgs(List<File> roots, List<File> outputRoots, Filters filters) {
    this.roots = roots;
    this.outputRoots = outputRoots;
    this.filters = filters;
  }

  public static InstrumentatorArgs from(String[] args) throws ArgParseException {
    final File argsFile = ReporterArgs.getArgsFile(args);
    try {
      return parse(argsFile);
    } catch (IOException e) {
      throw new ArgParseException(e);
    } catch (JSONException e) {
      throw new ArgParseException("Incorrect arguments in file " + argsFile.getAbsolutePath(), e);
    }
  }

  public static InstrumentatorArgs parse(File argsFile) throws IOException, ArgParseException {
    final String jsonString = FileUtils.readAll(argsFile);
    final JSONObject args = new JSONObject(jsonString);

    final Filters filters = ReporterArgs.parseFilters(args);
    final List<File> roots = ReporterArgs.parsePathList(args, ReporterArgs.OUTPUTS_TAG);
    final List<File> outputRoots = ReporterArgs.parsePathList(args, OUTPUT_COPY_TAG);

    if (roots.size() != outputRoots.size()) {
      throw new ArgParseException("Roots and outputRoots lists must have equal size");
    }

    return new InstrumentatorArgs(roots, outputRoots, filters);
  }

  public static String getHelpString() {
    return "Arguments must be passed in the following JSON format:\n" +
        "{\n" +
        "  include: {\n" +
        "        classes: [\"regex1\", \"regex2\"] [OPTIONAL]\n" +
        "   } [OPTIONAL],\n" +
        "  exclude: {\n" +
        "        classes: [\"regex1\", \"regex2\"], [OPTIONAL]\n" +
        "        annotations: [\"regex1\", \"regex2\"] [OPTIONAL]\n" +
        "   } [OPTIONAL],\n" +
        "   output: [\"path1\", \"path2\"]\n" +
        "   outputCopy: [\"path1\", \"path2\"]\n" +
        "}";
  }
}
