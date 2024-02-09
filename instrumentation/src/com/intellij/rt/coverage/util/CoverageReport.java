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

package com.intellij.rt.coverage.util;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.InstrumentationOptions;
import com.intellij.rt.coverage.instrumentation.UnloadedUtil;
import com.intellij.rt.coverage.instrumentation.data.ProjectContext;
import org.jetbrains.coverage.gnu.trove.TObjectIntHashMap;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Save coverage report in specific binary format.
 *
 * @author anna
 * @since 26-Feb-2010
 */
public class CoverageReport {

  /**
   * Saves project data into a coverage report.
   * This method firstly collect all internal information to be ready for save.
   */
  public static void save(ProjectData projectData, ProjectContext projectContext) {
    CoverageIOUtil.FileLock lock = null;
    try {
      finalizeCoverage(projectData, projectContext);

      InstrumentationOptions options = projectContext.getOptions();
      lock = CoverageIOUtil.FileLock.lock(options.dataFile);
      if (options.isMergeData) {
        try {
          ProjectData load = ProjectDataLoader.load(options.dataFile);
          projectData.merge(load);
        } catch (OutOfMemoryError e) {
          ErrorReporter.warn("Out of memory error occurred during previous report loading. Try to increase memory available for the JVM or write new report to a separate file", e);
        }
      }

      save(projectData, options);
    } catch (OutOfMemoryError e) {
      ErrorReporter.warn("Out of memory error occurred, try to increase memory available for the JVM, or make include / exclude patterns more specific", e);
    } catch (Throwable e) {
      ErrorReporter.warn("Unexpected error during report saving", e);
    } finally {
      CoverageIOUtil.FileLock.unlock(lock);
    }
  }

  public static void finalizeCoverage(ProjectData projectData, ProjectContext projectContext) {
    InstrumentationOptions options = projectContext.getOptions();
    projectData.setIncludePatterns(options.includePatterns);
    projectData.setExcludePatterns(options.excludePatterns);
    projectData.setAnnotationsToIgnore(options.excludeAnnotations);
    projectData.setInstructionsCoverage(options.isInstructionCoverage);

    projectContext.applyHits(projectData);
    if (options.isCalculateUnloaded) {
      UnloadedUtil.appendUnloaded(projectData, projectContext);
    }
    projectContext.applyLineMappings(projectData);
    projectContext.dropIgnoredLines(projectData);
  }

  public static void save(ProjectData projectData, InstrumentationOptions options) {
    DataOutputStream os = null;
    try {
      os = CoverageIOUtil.openWriteFile(options.dataFile);
      final TObjectIntHashMap<String> dict = new TObjectIntHashMap<String>();
      final Map<String, ClassData> classes = new HashMap<String, ClassData>(projectData.getClasses());
      CoverageIOUtil.writeINT(os, classes.size());
      saveDictionary(os, dict, classes);
      saveData(os, dict, classes);

      CoverageIOUtil.writeINT(os, ProjectDataLoader.REPORT_VERSION);
      CoverageIOUtil.writeUTF(os, getExtraInfoString());
      ReportSectionsUtil.saveSections(projectData, os, dict, options);

      saveSourceMap(classes, options.sourceMapFile);
    } catch (IOException e) {
      ErrorReporter.warn("Error writing file " + options.dataFile.getPath(), e);
    } finally {
      CoverageIOUtil.close(os);
    }
  }

  /**
   * This line may contain some useful configuration for sections parsing.
   * This field is string type to be extended easily.If a new agent version relies on this line data,
   * it must be extended such that it is possible to parse it and use for further extensions.
   */
  private static String getExtraInfoString() {
    return "";
  }

  static void saveSourceMap(Map<String, ClassData> classes, File sourceMapFile) {
    if (sourceMapFile == null) return;
    Map<String, String> readNames = Collections.emptyMap();
    try {
      if (sourceMapFile.exists() && sourceMapFile.length() > 0) {
        readNames = loadSourceMapFromFile(classes, sourceMapFile);
      }
    } catch (IOException e) {
      ErrorReporter.warn("Error loading source map from " + sourceMapFile.getPath(), e);
    }

    try {
      doSaveSourceMap(readNames, sourceMapFile, classes);
    } catch (IOException e) {
      ErrorReporter.warn("Error writing source map " + sourceMapFile.getPath(), e);
    }
  }

  @SuppressWarnings("unused") // used in IntelliJ
  public static void loadAndApplySourceMap(ProjectData projectData, File sourceMapFile) throws IOException {
    final Map<String, String> map = loadSourceMapFromFile(new HashMap<String, ClassData>(), sourceMapFile);
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String className = entry.getKey();
      String source = entry.getValue();
      ClassData data = projectData.getClassData(className);
      if (data != null) {
        data.setSource(source);
      }
    }
  }

  static Map<String, String> loadSourceMapFromFile(Map<String, ClassData> classes, File sourceMapFile) throws IOException {
    DataInputStream in = null;
    try {
      in = new DataInputStream(new FileInputStream(sourceMapFile));
      final int classNumber = CoverageIOUtil.readINT(in);
      final HashMap<String, String> readNames = new HashMap<String, String>(classNumber);
      for (int i = 0; i < classNumber; ++i) {
        final String className = CoverageIOUtil.readUTFFast(in);
        final String classSource = CoverageIOUtil.readUTFFast(in);
        if ("".equals(classSource)) {
          continue;
        }
        final ClassData data = classes.get(className);
        if (data == null || data.getSource() == null || !data.getSource().equals(classSource)) {
          readNames.put(className, classSource);
        }
      }
      return readNames;
    } finally {
      CoverageIOUtil.close(in);
    }
  }

  private static void saveData(DataOutputStream os, final TObjectIntHashMap<String> dict, Map<String, ClassData> classes) throws IOException {
    for (ClassData o : classes.values()) {
      o.save(os, new DictionaryLookup() {
        public int getDictionaryIndex(String className) {
          return dict.containsKey(className) ? dict.get(className) : -1;
        }
      });
    }
  }

  private static void saveDictionary(DataOutputStream os, TObjectIntHashMap<String> dict, Map<String, ClassData> classes) throws IOException {
    int i = 0;
    for (String className : classes.keySet()) {
      dict.put(className, i++);
      CoverageIOUtil.writeUTF(os, className);
    }
  }

  static void doSaveSourceMap(Map<String, String> classNameToFile, File sourceMapFile, Map<String, ClassData> classes) throws IOException {
    final HashMap<String, String> sources = new HashMap<String, String>(classNameToFile);
    for (ClassData classData : classes.values()) {
      if (!sources.containsKey(classData.getName())) {
        sources.put(classData.getName(), classData.getSource());
      }
    }

    DataOutputStream out = null;
    try {
      out = CoverageIOUtil.openWriteFile(sourceMapFile);
      CoverageIOUtil.writeINT(out, sources.size());
      for (Map.Entry<String, String> entry : sources.entrySet()) {
        CoverageIOUtil.writeUTF(out, entry.getKey());
        final String value = entry.getValue();
        CoverageIOUtil.writeUTF(out, value != null ? value : "");
      }
    } finally {
      CoverageIOUtil.close(out);
    }
  }
}
