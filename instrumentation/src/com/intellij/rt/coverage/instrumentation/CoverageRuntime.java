/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.util.ErrorReporter;
import com.intellij.rt.coverage.util.MethodCaller;

import java.util.Arrays;
import java.util.Map;

/**
 * This class is used to access coverage data at runtime.
 */
public class CoverageRuntime {

  public static final String COVERAGE_RUNTIME_OWNER = "com/intellij/rt/coverage/instrumentation/CoverageRuntime";
  private static final MethodCaller GET_HITS_MASK_METHOD = new MethodCaller("getHitsMask", new Class[]{String.class});
  private static final MethodCaller GET_HITS_MASK_CACHED_METHOD = new MethodCaller("getHitsMaskCached", new Class[]{String.class});
  private static final MethodCaller GET_TRACE_MASK_METHOD = new MethodCaller("getTraceMask", new Class[]{String.class});
  private static final MethodCaller LOAD_CLASS_DATA_METHOD = new MethodCaller("loadClassData", new Class[]{String.class});
  private static final MethodCaller LOAD_CLASS_DATA_CACHED_METHOD = new MethodCaller("loadClassDataCached", new Class[]{String.class});
  private static final MethodCaller REGISTER_CLASS_FOR_TRACE_METHOD = new MethodCaller("registerClassForTrace", new Class[]{Object.class});
  private static final MethodCaller TRACE_LINE_METHOD = new MethodCaller("traceLine", new Class[]{Object.class, int.class});

  /**
   * Cached object for ProjectData access from user class loaders.
   */
  private static Object ourRuntimeObject;
  public static CoverageRuntime ourRuntime;

  public final ProjectData myProjectData;
  private ClassesMap myClassesMap;

  private CoverageRuntime(ProjectData projectData) {
    myProjectData = projectData;
  }

  public static void installRuntime(ProjectData projectData) {
    ourRuntime = new CoverageRuntime(projectData);
    ProjectData.ourProjectData = projectData;
  }

  private ClassesMap getClassesMap() {
    ClassesMap map = myClassesMap;
    if (map == null) {
      synchronized (this) {
        map = myClassesMap;
        if (map == null) {
          map = new ClassesMap();
          myClassesMap = map;
        }
      }
    }
    return map;
  }


  // -----------------------  used from instrumentation  -------------------------------------------------//
  // load CoverageRuntime always through system class loader (null) then user's ClassLoaders won't affect //
  // IMPORTANT: do not remove reflection, it was introduced to avoid ClassCastExceptions in CoverageData  //
  // loaded via user's class loader                                                                       //
  // -----------------------------------------------------------------------------------------------------//

  /**
   * Mark line as covered in the current test during test tracking.
   */
  @SuppressWarnings("unused")
  public static void traceLine(Object classData, int line) {
    if (ourRuntime != null) {
      final Map<Object, boolean[]> traces = ourRuntime.myProjectData.getTraces();
      if (traces != null) {
        final boolean[] lines = ourRuntime.myProjectData.traceLineByTest((ClassData) classData, line);
        if (lines != null) {
          traces.put(classData, lines);
        }
      }
      return;
    }
    try {
      final Object runtimeObject = getRuntimeObject();
      TRACE_LINE_METHOD.invoke(runtimeObject, new Object[]{classData, line});
    } catch (Exception e) {
      ErrorReporter.reportError("Error during test tracking in class " + classData.toString(), e);
    }
  }

  /**
   * Test tracking initialization.
   * Returns true if a test is running now, then the class has been registered.
   */
  @SuppressWarnings("unused")
  public static void registerClassForTrace(Object classData) {
    if (ourRuntime != null) {
      final Map<Object, boolean[]> traces = ourRuntime.myProjectData.getTraces();
      if (traces != null) {
        synchronized (classData) {
          final boolean[] trace = ((ClassData) classData).getTraceMask();
          if (traces.put(classData, trace) == null) {
            // clear trace on register for a new test to prevent reporting about code running between tests
            Arrays.fill(trace, false);
          }
          trace[0] = true;
        }
      }
      return;
    }
    try {
      final Object runtimeObject = getRuntimeObject();
      REGISTER_CLASS_FOR_TRACE_METHOD.invoke(runtimeObject, new Object[]{classData});
    } catch (Exception e) {
      ErrorReporter.reportError("Error during test tracking in class " + classData.toString(), e);
    }
  }

  /**
   * On class initialization at runtime, an instrumented class asks for hits array
   */
  public static int[] getHitsMask(String className) {
    CoverageRuntime runtime = ourRuntime;
    if (runtime != null) {
      return runtime.myProjectData.getClassData(className).getHitsMask();
    }
    try {
      final Object runtimeObject = getRuntimeObject();
      return (int[]) GET_HITS_MASK_METHOD.invoke(runtimeObject, new Object[]{className});
    } catch (Exception e) {
      ErrorReporter.reportError("Error in class data access: " + className, e);
      return null;
    }
  }

  /**
   * On class initialization at runtime, an instrumented class asks for hits array
   * This version is used cache when calls are frequent.
   */
  @SuppressWarnings("unused")
  public static int[] getHitsMaskCached(String className) {
    CoverageRuntime runtime = ourRuntime;
    if (runtime != null) {
      return runtime.getClassesMap().get(className, runtime.myProjectData).getHitsMask();
    }
    try {
      final Object runtimeObject = getRuntimeObject();
      return (int[]) GET_HITS_MASK_CACHED_METHOD.invoke(runtimeObject, new Object[]{className});
    } catch (Exception e) {
      ErrorReporter.reportError("Error in class data access: " + className, e);
      return null;
    }
  }

  /**
   * Get test tracking hits array at runtime.
   */
  public static boolean[] getTraceMask(String className) {
    CoverageRuntime runtime = ourRuntime;
    if (runtime != null) {
      return runtime.myProjectData.getClassData(className).getTraceMask();
    }
    try {
      final Object runtimeObject = getRuntimeObject();
      return (boolean[]) GET_TRACE_MASK_METHOD.invoke(runtimeObject, new Object[]{className});
    } catch (Exception e) {
      ErrorReporter.reportError("Error in class data access: " + className, e);
      return null;
    }
  }

  /**
   * Get class data object at runtime.
   */
  public static Object loadClassData(String className) {
    CoverageRuntime runtime = ourRuntime;
    if (runtime != null) {
      return runtime.myProjectData.getClassData(className);
    }
    try {
      final Object runtimeObject = getRuntimeObject();
      return LOAD_CLASS_DATA_METHOD.invoke(runtimeObject, new Object[]{className});
    } catch (Exception e) {
      ErrorReporter.reportError("Error in class data loading: " + className, e);
      return null;
    }
  }

  /**
   * Get class data object at runtime.
   * This version is used cache when calls are frequent.
   */
  @SuppressWarnings("unused")
  public static Object loadClassDataCached(String className) {
    CoverageRuntime runtime = ourRuntime;
    if (runtime != null) {
      return runtime.getClassesMap().get(className, runtime.myProjectData);
    }
    try {
      final Object runtimeObject = getRuntimeObject();
      return LOAD_CLASS_DATA_CACHED_METHOD.invoke(runtimeObject, new Object[]{className});
    } catch (Exception e) {
      ErrorReporter.reportError("Error in class data loading: " + className, e);
      return null;
    }
  }

  private static Object getRuntimeObject() throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException {
    if (ourRuntimeObject == null) {
      final Class<?> runtimeClass = Class.forName(CoverageRuntime.class.getName(), false, null);
      ourRuntimeObject = runtimeClass.getDeclaredField("ourRuntime").get(null);
    }
    return ourRuntimeObject;
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
    private final IdentityClassData[] myIdentityArray = new IdentityClassData[POOL_SIZE];

    public ClassData get(String name, ProjectData projectData) {
      int idx = name.hashCode() & MASK;
      final IdentityClassData lastClassData = myIdentityArray[idx];
      if (lastClassData != null) {
        final ClassData data = lastClassData.getClassData(name);
        if (data != null) return data;
      }

      final ClassData data = projectData.getClassData(name);
      myIdentityArray[idx] = new IdentityClassData(name, data);
      return data;
    }
  }

  private static class IdentityClassData {
    private final String myClassName;
    private final ClassData myClassData;

    private IdentityClassData(String className, ClassData classData) {
      myClassName = className;
      myClassData = classData;
    }

    @SuppressWarnings("StringEquality")
    public ClassData getClassData(String name) {
      if (name == myClassName) {
        return myClassData;
      }
      return null;
    }
  }
}
