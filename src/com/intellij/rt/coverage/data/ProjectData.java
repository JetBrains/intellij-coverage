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

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Represents coverage information of a whole project.
 */
public class ProjectData implements CoverageData, Serializable {
  public static ProjectData ourProjectData;

  private List<Pattern> myIncludePatterns;
  private List<Pattern> myExcludePatterns;
  private List<Pattern> myAnnotationsToIgnore;

  private final Map<String, ClassData> myClasses = new ConcurrentHashMap<String, ClassData>(1000);
  private boolean myCollectInstructions;
  private Map<String, ClassInstructions> myInstructions;

  public final TestTrackingCallback myTestTrackingCallback;

  public ProjectData() {
    this(null);
  }

  public ProjectData(TestTrackingCallback testTrackingCallback) {
    myTestTrackingCallback = testTrackingCallback;
  }

  public ClassData getClassData(final String name) {
    return myClasses.get(name);
  }

  public ClassData getOrCreateClassData(String name) {
    ClassData classData = myClasses.get(name);
    if (classData == null) {
      classData = new ClassData(name);
      myClasses.put(name, classData);
    }
    return classData;
  }

  public void addClassData(ClassData classData) {
    myClasses.put(classData.getName(), classData);
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

  public List<Pattern> getIncludePatterns() {
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

  public boolean isInstructionsCoverageEnabled() {
    return myCollectInstructions;
  }

  public void setInstructionsCoverage(boolean collectInstructions) {
    myCollectInstructions = collectInstructions;
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

  // --------------- used from listeners --------------------- //

  public static ProjectData getProjectData() {
    return ourProjectData;
  }

  /**
   * This method could be called in test tracking mode by test engine listeners
   */
  public void testEnded(final String name) {
    if (myTestTrackingCallback != null) myTestTrackingCallback.testEnded(name);
  }

  /**
   * This method could be called in test tracking mode by test engine listeners
   */
  public void testStarted(final String name) {
    if (myTestTrackingCallback != null) myTestTrackingCallback.testStarted(name);
  }
  //---------------------------------------------------------- //

}
