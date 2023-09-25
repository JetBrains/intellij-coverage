/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.rt.coverage.data;

import com.intellij.rt.coverage.data.instructions.ClassInstructions;
import com.intellij.rt.coverage.data.instructions.InstructionsUtil;
import com.intellij.rt.coverage.util.*;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Represents coverage information of a whole project.
 */
public class ProjectData implements CoverageData, Serializable {
  public static ProjectData ourProjectData;

  private final File myDataFile;
  private final boolean myBranchCoverage;
  private List<Pattern> myIncludePatterns;
  private List<Pattern> myExcludePatterns;
  private List<Pattern> myAnnotationsToIgnore;
  private final Map<String, ClassData> myClasses = new ConcurrentHashMap<String, ClassData>(1000);
  private final StringsPool myStringPool = new StringsPool();
  private final IgnoredStorage myIgnoredStorage = new IgnoredStorage();
  private volatile Map<String, FileMapData[]> myLinesMap;

  /**
   * Test tracking trace storage. Test tracking supports only sequential tests (but code inside one test could be parallel).
   * Nevertheless, in case of parallel tests run setting storage to null truncates coverage significantly.
   * Using CAS for the storage update slightly improves test tracking coverage as the data are not cleared too frequently.
   */
  private final AtomicReference<Map<Object, boolean[]>> myTrace = new AtomicReference<Map<Object, boolean[]>>();
  private final TestTrackingCallback myTestTrackingCallback;
  private File myTracesDir;

  private boolean myCollectInstructions;
  private Map<String, ClassInstructions> myInstructions;

  private boolean myStopped;

  public ProjectData() {
    this(null, true, null);
  }

  public ProjectData(File dataFile,
                     boolean branchCoverage,
                     TestTrackingCallback testTrackingCallback) {
    myDataFile = dataFile;
    myBranchCoverage = branchCoverage;
    myTestTrackingCallback = testTrackingCallback;
  }

  public ClassData getClassData(final String name) {
    return myClasses.get(name);
  }

  public ClassData getOrCreateClassData(String name) {
    name = getFromPool(name);
    ClassData classData = myClasses.get(name);
    if (classData == null) {
      classData = new ClassData(name);
      myClasses.put(name, classData);
    }
    return classData;
  }

  public int getClassesNumber() {
    return myClasses.size();
  }

  public Map<String, ClassData> getClasses() {
    return new HashMap<String, ClassData>(myClasses);
  }

  public Collection<ClassData> getClassesCollection() {
    return myClasses.values();
  }

  public void stop() {
    myStopped = true;
  }

  public boolean isStopped() {
    return myStopped;
  }

  public boolean isBranchCoverage() {
    return myBranchCoverage;
  }

  public boolean isTestTracking() {
    return myTestTrackingCallback != null;
  }

  public boolean isInstructionsCoverageEnabled() {
    return myCollectInstructions;
  }

  public void setInstructionsCoverage(boolean collectInstructions) {
    myCollectInstructions = collectInstructions;
  }

  public Map<String, FileMapData[]> getLinesMap() {
    return myLinesMap;
  }

  public Map<String, ClassInstructions> getInstructions() {
    Map<String, ClassInstructions> instructions = myInstructions;
    if (instructions == null) {
      synchronized (this) {
        instructions = myInstructions;
        if (instructions == null) {
          instructions = new ConcurrentHashMap<String, ClassInstructions>();
          myInstructions = instructions;
        }
      }
    }
    return instructions;
  }

  public List<Pattern> getIncudePatterns() {
    return myIncludePatterns;
  }

  public void setIncludePatterns(List<Pattern> patterns) {
    myIncludePatterns = patterns;
  }

  public List<Pattern> getExcludePatterns() {
    return myExcludePatterns;
  }

  public void setExcludePatterns(List<Pattern> patterns) {
    myExcludePatterns = patterns;
  }

  public List<Pattern> getAnnotationsToIgnore() {
    return myAnnotationsToIgnore;
  }

  public void setAnnotationsToIgnore(List<Pattern> annotations) {
    myAnnotationsToIgnore = annotations;
  }

  public IgnoredStorage getIgnoredStorage() {
    return myIgnoredStorage;
  }

  public String getFromPool(String s) {
    return myStringPool.getFromPool(s);
  }

  public Map<Object, boolean[]> getTraces() {
    return myTrace.get();
  }

  public boolean[] traceLineByTest(ClassData classData, int line) {
    if (myTestTrackingCallback == null) return null;
    return myTestTrackingCallback.traceLine(classData, line);
  }

  public void merge(final CoverageData data) {
    final ProjectData projectData = (ProjectData) data;
    for (Map.Entry<String, ClassData> entry : projectData.myClasses.entrySet()) {
      final String key = entry.getKey();
      final ClassData mergedData = entry.getValue();
      ClassData classData = myClasses.get(key);
      if (classData == null) {
        classData = new ClassData(mergedData.getName());
        myClasses.put(key, classData);
      }
      classData.merge(mergedData);
    }

    InstructionsUtil.merge(projectData, this, null);
  }

  /**
   * Remove all lines that are generated by inline.
   * Should be called only in case when hits of these lines are out of interest,
   * foe example when analysing unloaded classes.
   */
  public void dropLineMappings() {
    if (myLinesMap == null) return;
    for (Map.Entry<String, FileMapData[]> entry : myLinesMap.entrySet()) {
      final ClassData classData = getClassData(entry.getKey());
      final FileMapData[] mappings = entry.getValue();
      classData.dropMappedLines(mappings);
      InstructionsUtil.dropMappedLines(this, classData.getName(), mappings);
    }
  }

  /**
   * Apply line mappings: move hits from original line in bytecode to the mapped line.
   */
  public void applyLineMappings() {
    if (myLinesMap != null) {
      for (Map.Entry<String, FileMapData[]> entry : myLinesMap.entrySet()) {
        final String className = entry.getKey();
        final ClassData classData = getClassData(className);
        final FileMapData[] fileData = entry.getValue();
        //postpone process main file because its lines would be reset and next files won't be processed correctly
        FileMapData mainData = null;
        for (FileMapData aFileData : fileData) {
          final String mappedClassName = aFileData.getClassName();
          if (mappedClassName.equals(className)) {
            mainData = aFileData;
            continue;
          }
          final ClassData classInfo;
          if (!ClassNameUtil.matchesPatterns(mappedClassName, myExcludePatterns)
              && (myIncludePatterns == null || myIncludePatterns.isEmpty() || ClassNameUtil.matchesPatterns(mappedClassName, myIncludePatterns))) {
            classInfo = getOrCreateClassData(mappedClassName);
            if (classInfo.getSource() == null || classInfo.getSource().isEmpty()) {
              classInfo.setSource(aFileData.getFileName());
            }
          } else {
            // `classData` SMAP may not contain mapping to itself,
            // so it's better to make sure we fairly apply this mapping
            // otherwise `classData` may contain inline generated lines
            classInfo = new ClassData(mappedClassName);
          }
          ClassData.checkLineMappings(aFileData.getLines(), classInfo, classData);
          InstructionsUtil.applyInstructionsSMAP(this, aFileData.getLines(), classInfo, classData);
        }

        if (mainData != null) {
          ClassData.checkLineMappings(mainData.getLines(), classData, classData);
          InstructionsUtil.applyInstructionsSMAP(this, mainData.getLines(), classData, classData);
        }
      }
    }
  }

  /**
   * Update coverage data internally stored in arrays.
   */
  public void applyHits() {
    for (ClassData data : myClasses.values()) {
      data.applyHits();
    }
  }

  public void dropIgnoredLines() {
    for (final ClassData classData : getClassesCollection()) {
      classData.dropIgnoredLines();
    }
  }

  public void addLineMaps(String className, FileMapData[] fileDatas) {
    if (myLinesMap == null) {
      synchronized (FileMapData.class) {
        if (myLinesMap == null) {
          myLinesMap = new ConcurrentHashMap<String, FileMapData[]>();
        }
      }
    }
    myLinesMap.put(className, fileDatas);
  }

  // --------------- used from listeners --------------------- //

  public static ProjectData getProjectData() {
    return ourProjectData;
  }

  /**
   * This method could be called in test tracking mode by test engine listeners
   */
  public void testEnded(final String name) {
    final Map<Object, boolean[]> trace = myTrace.getAndSet(null);
    if (trace == null) return;
    File tracesDir = getTracesDir();
    try {
      TestTrackingIOUtil.saveTestResults(tracesDir, name, trace);
    } catch (IOException e) {
      ErrorReporter.warn("Error writing traces for test '" + name + "' to directory " + tracesDir.getPath(), e);
    } finally {
      for (Map.Entry<Object, boolean[]> entry : trace.entrySet()) {
        final ClassData classData = (ClassData) entry.getKey();
        final boolean[] touched = entry.getValue();
        final Object[] lines = classData.getLines();
        final int lineCount = Math.min(lines.length, touched.length);
        for (int i = 1; i < lineCount; i++) {
          final LineData lineData = (LineData) lines[i];
          if (lineData == null || !touched[i]) continue;
          lineData.setTestName(name);
        }
        myTestTrackingCallback.clearTrace(classData);
      }
    }
  }

  /**
   * This method could be called in test tracking mode by test engine listeners
   */
  public void testStarted(final String ignoredName) {
    if (isTestTracking()) myTrace.compareAndSet(null, new ConcurrentHashMap<Object, boolean[]>());
  }
  //---------------------------------------------------------- //


  private File getTracesDir() {
    if (myTracesDir == null) {
      myTracesDir = createTracesDir(myDataFile);
    }
    return myTracesDir;
  }

  public static File createTracesDir(File dataFile) {
    final String fileName = dataFile.getName();
    final int i = fileName.lastIndexOf('.');
    final String dirName = i != -1 ? fileName.substring(0, i) : fileName;
    final File result = new File(dataFile.getParent(), dirName);
    if (!result.exists()) {
      result.mkdirs();
    }
    return result;
  }
}
