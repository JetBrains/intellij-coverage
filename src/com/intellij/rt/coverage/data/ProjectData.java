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

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class ProjectData implements CoverageData, Serializable {
  public static final String PROJECT_DATA_OWNER = "com/intellij/rt/coverage/data/ProjectData";

  // ClassData methods
  private static final MethodCaller TOUCH_LINE_METHOD = new MethodCaller("touchLine", new Class[] {int.class});
  private static final MethodCaller GET_LINE_MASK_METHOD = new MethodCaller("getLineMask", new Class[0]);
  private static final MethodCaller GET_HITS_MASK_METHOD = new MethodCaller("getHitsMask", new Class[0]);
  private static final MethodCaller GET_TRACE_MASK_METHOD = new MethodCaller("getTraceMask", new Class[0]);
  private static final MethodCaller TOUCH_SWITCH_METHOD = new MethodCaller("touch", new Class[] {int.class, int.class, int.class});
  private static final MethodCaller TOUCH_JUMP_METHOD = new MethodCaller("touch", new Class[] {int.class, int.class, boolean.class});
  private static final MethodCaller TOUCH_METHOD = new MethodCaller("touch", new Class[] {int.class});

  // ProjectData methods
  private static final MethodCaller GET_CLASS_DATA_METHOD = new MethodCaller("getClassData", new Class[]{String.class});
  private static final MethodCaller REGISTER_CLASS_FOR_TRACE_METHOD = new MethodCaller("registerClassForTrace", new Class[]{Object.class});
  private static final MethodCaller TRACE_LINE_METHOD = new MethodCaller("traceLine", new Class[]{Object.class, int.class});

  private static boolean ourStopped = false;

  public static ProjectData ourProjectData;
  private File myDataFile;

  private boolean myTraceLines;
  private boolean mySampling;
  private boolean myCollectInstructions;

  /**
   * Test tracking trace storage. Test tracking supports only sequential tests (but code inside one test could be parallel).
   * Nevertheless in case of parallel tests run setting storage to null truncates coverage significantly.
   * Using CAS for the storage update slightly improves test tracking coverage as the data are not cleared too frequently.
   */
  private final AtomicReference<Map<Object, boolean[]>> myTrace = new AtomicReference<Map<Object, boolean[]>>();
  private File myTracesDir;
  private List<Pattern> myIncludePatterns;
  private List<Pattern> myExcludePatterns;

  private final ClassesMap myClasses = new ClassesMap();
  private volatile Map<String, FileMapData[]> myLinesMap;
  private Map<String, ClassInstructions> myInstructions;

  private static Object ourProjectDataObject;

  private TestTrackingCallback myTestTrackingCallback;

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
    ourStopped = true;
  }

  public boolean isStopped() {
    return ourStopped;
  }

  public boolean isSampling() {
    return mySampling;
  }

  public boolean isTestTracking() {
    return myTraceLines;
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

  public static ProjectData createProjectData(final File dataFile,
                                              final ProjectData initialData,
                                              boolean traceLines,
                                              boolean isSampling,
                                              List<Pattern> includePatterns,
                                              List<Pattern> excludePatterns,
                                              final TestTrackingCallback testTrackingCallback) throws IOException {
    ourProjectData = initialData == null ? new ProjectData() : initialData;
    if (dataFile != null && !dataFile.exists()) {
      final File parentDir = dataFile.getParentFile();
      if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();
      dataFile.createNewFile();
    }
    ourProjectData.mySampling = isSampling;
    ourProjectData.myTraceLines = traceLines;
    ourProjectData.myCollectInstructions = OptionsUtil.INSTRUCTIONS_COVERAGE_ENABLED;
    ourProjectData.myDataFile = dataFile;
    ourProjectData.myIncludePatterns = includePatterns;
    ourProjectData.myExcludePatterns = excludePatterns;
    ourProjectData.myTestTrackingCallback = testTrackingCallback;
    return ourProjectData;
  }

  public void merge(final CoverageData data) {
    final ProjectData projectData = (ProjectData)data;
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
  public void checkLineMappings() {
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

  public void applyLinesMask() {
    if (!mySampling) return;
    for (ClassData data : myClasses.myClasses.values()) {
      data.applyLinesMask();
    }
  }

  public void applyBranchData() {
    if (mySampling) return;
    for (ClassData data : myClasses.myClasses.values()) {
      data.applyBranches();
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
        final Object[] lines = classData.getLines();
        final int lineCount = Math.min(lines.length, touched.length);
        for (int i = 1; i < lineCount; i++) {
          final LineData lineData = (LineData) lines[i];
          if (lineData == null || !touched[i]) continue;
          lineData.setTestName(name);
        }
        myTestTrackingCallback.clearTrace(classData);
      }
      myTrace.compareAndSet(trace, null);
    }
  }

  public void testStarted(final String name) {
    if (myTraceLines) myTrace.compareAndSet(null, new ConcurrentHashMap<Object, boolean[]>());
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

  /** @noinspection UnusedDeclaration*/
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

  public static void touchLine(Object classData, int line) {
    if (ourProjectData != null) {
      ((ClassData) classData).touchLine(line);
      return;
    }
    touch(TOUCH_LINE_METHOD,
          classData,
          new Object[]{line});
  }

  public static void touchSwitch(Object classData, int line, int switchNumber, int key) {
    if (ourProjectData != null) {
      ((ClassData) classData).touch(line, switchNumber, key);
      return;
    }
    touch(TOUCH_SWITCH_METHOD,
          classData,
          new Object[]{line, switchNumber, key});
  }

  public static void touchJump(Object classData, int line, int jump, boolean hit) {
    if (ourProjectData != null) {
      ((ClassData) classData).touch(line, jump, hit);
      return;
    }
    touch(TOUCH_JUMP_METHOD,
          classData,
          new Object[]{line, jump, hit});
  }

  public static void trace(Object classData, int line) {
    traceLine(classData, line);
    if (ourProjectData != null) {
      ((ClassData) classData).touch(line);
      return;
    }

    touch(TOUCH_METHOD,
          classData,
          new Object[]{line});
  }

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
      ErrorReporter.reportError("Error tracing class " + classData.toString(), e);
    }
  }

  /**
   * Returns true if a test is running now, then the class has been registered.
   */
  public static boolean registerClassForTrace(Object classData) {
    if (ourProjectData != null) {
      final Map<Object, boolean[]> traces = ourProjectData.myTrace.get();
      if (traces != null) {
        synchronized (classData) {
          final boolean[] trace = ((ClassData)classData).getTraceMask();
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
      ErrorReporter.reportError("Error tracing class " + classData.toString(), e);
      return false;
    }
  }

  private static Object touch(final MethodCaller methodCaller, Object classData, final Object[] paramValues) {
    try {
      return methodCaller.invoke(classData, paramValues);
    } catch (Exception e) {
      ErrorReporter.reportError("Error in project data collection: " + methodCaller.myMethodName, e);
      return null;
    }
  }

  public static int[] getLineMask(String className) {
    if (ourProjectData != null) {
      return ourProjectData.getClassData(className).getLineMask();
    }
    try {
      final Object classData = getClassDataObject(className);
      return (int[]) touch(GET_LINE_MASK_METHOD, classData, new Object[0]);
    } catch (Exception e) {
      ErrorReporter.reportError("Error in class data loading: " + className, e);
      return null;
    }
  }

  public static int[] getHitsMask(String className) {
    if (ourProjectData != null) {
      return ourProjectData.getClassData(className).getHitsMask();
    }
    try {
      final Object classData = getClassDataObject(className);
      return (int[]) touch(GET_HITS_MASK_METHOD, classData, new Object[0]);
    } catch (Exception e) {
      ErrorReporter.reportError("Error in class data access: " + className, e);
      return null;
    }
  }

  public static boolean[] getTraceMask(String className) {
    if (ourProjectData != null) {
      return ourProjectData.getClassData(className).getTraceMask();
    }
    try {
      final Object classData = getClassDataObject(className);
      return (boolean[]) touch(GET_TRACE_MASK_METHOD, classData, new Object[0]);
    } catch (Exception e) {
      ErrorReporter.reportError("Error in class data access: " + className, e);
      return null;
    }
  }

  public static Object loadClassData(String className) {
    if (ourProjectData != null) {
      return ourProjectData.getClassData(className);
    }
    try {
      return getClassDataObject(className);
    } catch (Exception e) {
      ErrorReporter.reportError("Error in class data loading: " + className, e);
      return null;
    }
  }

  private static Object getProjectDataObject() throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException {
    if (ourProjectDataObject == null) {
      final Class projectDataClass = Class.forName(ProjectData.class.getName(), false, null);
      ourProjectDataObject = projectDataClass.getDeclaredField("ourProjectData").get(null);
    }
    return ourProjectDataObject;
  }

  private static Object getClassDataObject(String className) throws Exception {
    final Object projectDataObject = getProjectDataObject();
    return GET_CLASS_DATA_METHOD.invoke(projectDataObject, new Object[]{className});
  }

  // ----------------------------------------------------------------------------------------------- //

  private static class MethodCaller {
    private Method myMethod;
    private final String myMethodName;
    private final Class[] myParamTypes;

    private MethodCaller(final String methodName, final Class[] paramTypes) {
      myMethodName = methodName;
      myParamTypes = paramTypes;
    }

    public Object invoke(Object thisObj, final Object[] paramValues) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
      if (myMethod == null) {
        myMethod = findMethod(thisObj.getClass(), myMethodName, myParamTypes);
      }
      return myMethod.invoke(thisObj, paramValues);
    }

    private static Method findMethod(final Class<?> clazz, String name, Class[] paramTypes) throws NoSuchMethodException {
      Method m = clazz.getDeclaredMethod(name, paramTypes);
      // speedup method invocation by calling setAccessible(true)
      m.setAccessible(true);
      return m;
    }
  }

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
