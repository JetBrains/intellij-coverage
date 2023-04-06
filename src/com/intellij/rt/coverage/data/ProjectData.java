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
 * This class is also used to access coverage data at runtime.
 */
public class ProjectData implements CoverageData, Serializable {
  public static final String PROJECT_DATA_OWNER = "com/intellij/rt/coverage/data/ProjectData";

  // ProjectData methods
  private static final MethodCaller GET_HITS_MASK_METHOD = new MethodCaller("getHitsMask", new Class[]{String.class});
  private static final MethodCaller GET_TRACE_MASK_METHOD = new MethodCaller("getTraceMask", new Class[]{String.class});
  private static final MethodCaller GET_CLASS_DATA_METHOD = new MethodCaller("getClassData", new Class[]{String.class});
  private static final MethodCaller REGISTER_CLASS_FOR_TRACE_METHOD = new MethodCaller("registerClassForTrace", new Class[]{Object.class});
  private static final MethodCaller TRACE_LINE_METHOD = new MethodCaller("traceLine", new Class[]{Object.class, int.class});

  private boolean myStopped;

  public static ProjectData ourProjectData;
  private File myDataFile;

  private boolean myTestTracking;
  private boolean myBranchCoverage = true;
  private boolean myCollectInstructions;

  /**
   * Test tracking trace storage. Test tracking supports only sequential tests (but code inside one test could be parallel).
   * Nevertheless, in case of parallel tests run setting storage to null truncates coverage significantly.
   * Using CAS for the storage update slightly improves test tracking coverage as the data are not cleared too frequently.
   */
  private final AtomicReference<Map<Object, boolean[]>> myTrace = new AtomicReference<Map<Object, boolean[]>>();
  private File myTracesDir;
  private List<Pattern> myIncludePatterns;
  private List<Pattern> myExcludePatterns;

  private final ClassesMap myClasses = new ClassesMap();
  private volatile Map<String, FileMapData[]> myLinesMap;
  private Map<String, ClassInstructions> myInstructions;

  /**
   * Cached object for ProjectData access from user class loaders.
   */
  private static Object ourProjectDataObject;

  private TestTrackingCallback myTestTrackingCallback;

  private List<Pattern> myAnnotationsToIgnore;
  private Set<String> myIgnoredMethods;

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

  public static ProjectData getProjectData() {
    return ourProjectData;
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
    return myTestTracking;
  }

  public boolean isInstructionsCoverageEnabled() {
    return myCollectInstructions;
  }

  public void setInstructionsCoverage(boolean isEnabled) {
    myCollectInstructions = isEnabled;
  }

  public int getClassesNumber() {
    return myClasses.size();
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

  public List<Pattern> getAnnotationsToIgnore() {
    return myAnnotationsToIgnore;
  }

  public void setAnnotationsToIgnore(List<Pattern> annotations) {
    myAnnotationsToIgnore = annotations;
  }

  public synchronized boolean isMethodIgnored(String owner, String methodName, String desc) {
    return myIgnoredMethods != null && myIgnoredMethods.contains(createDesc(owner, methodName, desc));
  }

  public synchronized void addIgnoredMethod(String owner, String methodName, String desc) {
    if (myIgnoredMethods == null) {
      myIgnoredMethods = Collections.newSetFromMap(new HashMap<String, Boolean>());
    }
    final String methodDesc = createDesc(owner, methodName, desc);
    myIgnoredMethods.add(methodDesc);
  }

  private static String createDesc(String owner, String methodName, String desc) {
    return owner + "#" + methodName + desc;
  }


  public static ProjectData createProjectData(final File dataFile,
                                              final ProjectData initialData,
                                              boolean traceLines,
                                              boolean branchCoverage,
                                              List<Pattern> includePatterns,
                                              List<Pattern> excludePatterns,
                                              final TestTrackingCallback testTrackingCallback) throws IOException {
    ourProjectData = initialData == null ? new ProjectData() : initialData;
    if (dataFile != null && !dataFile.exists()) {
      final File parentDir = dataFile.getParentFile();
      if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();
      dataFile.createNewFile();
    }
    ourProjectData.myStopped = false;
    ourProjectData.myBranchCoverage = branchCoverage;
    ourProjectData.myTestTracking = traceLines;
    ourProjectData.myCollectInstructions = OptionsUtil.INSTRUCTIONS_COVERAGE_ENABLED;
    ourProjectData.myDataFile = dataFile;
    ourProjectData.myIncludePatterns = includePatterns;
    ourProjectData.myExcludePatterns = excludePatterns;
    ourProjectData.myTestTrackingCallback = testTrackingCallback;
    return ourProjectData;
  }

  public static ProjectData createProjectData(boolean branchCoverage) {
    final ProjectData projectData = new ProjectData();
    projectData.myBranchCoverage = branchCoverage;
    return projectData;
  }

  public void merge(final CoverageData data) {
    final ProjectData projectData = (ProjectData) data;
    for (Map.Entry<String, ClassData> entry : projectData.myClasses.myClasses.entrySet()) {
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
          if ((myExcludePatterns == null || !ClassNameUtil.matchesPatterns(mappedClassName, myExcludePatterns))
              && (myIncludePatterns == null || myIncludePatterns.isEmpty() || ClassNameUtil.matchesPatterns(mappedClassName, myIncludePatterns))) {
            classInfo = getOrCreateClassData(mappedClassName);
            if (classInfo.getSource() == null || classInfo.getSource().length() == 0) {
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
    for (ClassData data : myClasses.myClasses.values()) {
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

  /**
   * This method could be called in test tracking mode by test engine listeners
   */
  public void testEnded(final String name) {
    final Map<Object, boolean[]> trace = myTrace.get();
    if (trace == null) return;
    File tracesDir = getTracesDir();
    try {
      TestTrackingIOUtil.saveTestResults(tracesDir, name, trace);
    } catch (IOException e) {
      ErrorReporter.reportError("Error writing traces for test '" + name + "' to directory " + tracesDir.getPath(), e);
    } finally {
      for (Map.Entry<Object, boolean[]> entry : trace.entrySet()) {
        final ClassData classData = (ClassData) entry.getKey();
        final boolean[] touched = entry.getValue();
        final LineData[] lines = classData.getLines();
        final int lineCount = Math.min(lines.length, touched.length);
        for (int i = 1; i < lineCount; i++) {
          final LineData lineData = lines[i];
          if (lineData == null || !touched[i]) continue;
          lineData.setTestName(name);
        }
        myTestTrackingCallback.clearTrace(classData);
      }
      myTrace.compareAndSet(trace, null);
    }
  }

  /**
   * This method could be called in test tracking mode by test engine listeners
   */
  public void testStarted(final String ignoredName) {
    if (myTestTracking) myTrace.compareAndSet(null, new ConcurrentHashMap<Object, boolean[]>());
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

  public Map<String, ClassData> getClasses() {
    return myClasses.asMap();
  }

  public Collection<ClassData> getClassesCollection() {
    return myClasses.myClasses.values();
  }


  // -----------------------  used from instrumentation  ------------------------------------------------//

  //load ProjectData always through system class loader (null) then user's ClassLoaders won't affect    //
  //IMPORTANT: do not remove reflection, it was introduced to avoid ClassCastExceptions in CoverageData //
  //loaded via user's class loader                                                                      //

  // -------------------------------------------------------------------------------------------------- //

  /**
   * Mark line as covered in the current test during test tracking.
   */
  @SuppressWarnings("unused")
  public static void traceLine(Object classData, int line) {
    if (ourProjectData != null) {
      final Map<Object, boolean[]> traces = ourProjectData.myTrace.get();
      if (traces != null) {
        final boolean[] lines = ourProjectData.myTestTrackingCallback.traceLine((ClassData) classData, line);
        if (lines != null) {
          traces.put(classData, lines);
        }
      }
      return;
    }
    try {
      final Object projectData = getProjectDataObject();
      TRACE_LINE_METHOD.invoke(projectData, new Object[]{classData, line});
    } catch (Exception e) {
      ErrorReporter.reportError("Error during test tracking in class " + classData.toString(), e);
    }
  }

  /**
   * Test tracking initialization.
   * Returns true if a test is running now, then the class has been registered.
   */
  @SuppressWarnings("unused")
  public static boolean registerClassForTrace(Object classData) {
    if (ourProjectData != null) {
      final Map<Object, boolean[]> traces = ourProjectData.myTrace.get();
      if (traces != null) {
        synchronized (classData) {
          final boolean[] trace = ((ClassData) classData).getTraceMask();
          if (traces.put(classData, trace) == null) {
            // clear trace on register for a new test to prevent reporting about code running between tests
            Arrays.fill(trace, false);
          }
        }
        return true;
      }
      return false;
    }
    try {
      final Object projectData = getProjectDataObject();
      return (Boolean) REGISTER_CLASS_FOR_TRACE_METHOD.invoke(projectData, new Object[]{classData});
    } catch (Exception e) {
      ErrorReporter.reportError("Error during test tracking in class " + classData.toString(), e);
      return false;
    }
  }

  /**
   * On class initialization at runtime, an instrumented class asks for hits array
   */
  public static int[] getHitsMask(String className) {
    if (ourProjectData != null) {
      return ourProjectData.getClassData(className).getHitsMask();
    }
    try {
      final Object projectData = getProjectDataObject();
      return (int[]) GET_HITS_MASK_METHOD.invoke(projectData, new Object[]{className});
    } catch (Exception e) {
      ErrorReporter.reportError("Error in class data access: " + className, e);
      return null;
    }
  }

  /**
   * Get test tracking hits array at runtime.
   */
  @SuppressWarnings("unused")
  public static boolean[] getTraceMask(String className) {
    if (ourProjectData != null) {
      return ourProjectData.getClassData(className).getTraceMask();
    }
    try {
      final Object projectData = getProjectDataObject();
      return (boolean[]) GET_TRACE_MASK_METHOD.invoke(projectData, new Object[]{className});
    } catch (Exception e) {
      ErrorReporter.reportError("Error in class data access: " + className, e);
      return null;
    }
  }

  /**
   * Get class data object at runtime.
   */
  @SuppressWarnings("unused")
  public static Object loadClassData(String className) {
    if (ourProjectData != null) {
      return ourProjectData.getClassData(className);
    }
    try {
      final Object projectData = getProjectDataObject();
      return GET_CLASS_DATA_METHOD.invoke(projectData, new Object[]{className});
    } catch (Exception e) {
      ErrorReporter.reportError("Error in class data loading: " + className, e);
      return null;
    }
  }

  private static Object getProjectDataObject() throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException {
    if (ourProjectDataObject == null) {
      final Class<?> projectDataClass = Class.forName(ProjectData.class.getName(), false, null);
      ourProjectDataObject = projectDataClass.getDeclaredField("ourProjectData").get(null);
    }
    return ourProjectDataObject;
  }

  // ----------------------------------------------------------------------------------------------- //

  /**
   * This map provides faster read operations for the case when key is mostly the same
   * object. In our case key is the class name which is the same string with high probability.
   * According to CPU snapshots with usual map we spend a lot of time on equals() operation.
   * This class was introduced to reduce number of equals().
   */
  private static class ClassesMap {
    private static final int POOL_SIZE = 1024; // must be a power of two
    private static final int MASK = POOL_SIZE - 1;
    private static final int DEFAULT_CAPACITY = 1000;
    private final IdentityClassData[] myIdentityArray = new IdentityClassData[POOL_SIZE];
    private final Map<String, ClassData> myClasses = createClassesMap();

    public int size() {
      return myClasses.size();
    }

    public ClassData get(String name) {
      int idx = name.hashCode() & MASK;
      final IdentityClassData lastClassData = myIdentityArray[idx];
      if (lastClassData != null) {
        final ClassData data = lastClassData.getClassData(name);
        if (data != null) return data;
      }

      final ClassData data = myClasses.get(name);
      myIdentityArray[idx] = new IdentityClassData(name, data);
      return data;
    }

    public void put(String name, ClassData data) {
      myClasses.put(name, data);
    }

    public HashMap<String, ClassData> asMap() {
      return new HashMap<String, ClassData>(myClasses);
    }

    public Collection<String> names() {
      return myClasses.keySet();
    }

    private static Map<String, ClassData> createClassesMap() {
      if (OptionsUtil.THREAD_SAFE_STORAGE) {
        return new ConcurrentHashMap<String, ClassData>(DEFAULT_CAPACITY);
      }
      return new HashMap<String, ClassData>(DEFAULT_CAPACITY);
    }
  }

  private static class IdentityClassData {
    private final String myClassName;
    private final ClassData myClassData;

    private IdentityClassData(String className, ClassData classData) {
      myClassName = className;
      myClassData = classData;
    }

    public ClassData getClassData(String name) {
      if (name == myClassName) {
        return myClassData;
      }
      return null;
    }
  }

}
