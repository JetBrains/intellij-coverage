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

package com.intellij.rt.coverage.offline;

import com.intellij.rt.coverage.util.ErrorReporter;
import com.intellij.rt.coverage.util.MethodCaller;

import java.io.File;

/**
 * This class is an entry point from instrumented classes.
 * Here all the calls are redirected to system class loader.
 * Also, the project data is initialized on the first call.
 */
public class RawProjectInit {
  private static final MethodCaller GET_OR_CREATE_HITS_MASK_INTERNAL_METHOD = new MethodCaller("getOrCreateHitsInternal", new Class[]{String.class, int.class});

  private static final String KOVER_INITIALIZER_CLASS_NAME = "kotlinx.kover.offline.runtime.KoverInit";

  public static volatile RawProjectData ourProjectData;

  /**
   * This method is used in case of offline instrumentation.
   * As ProjectData is uninitialized, this method also creates ClassData when needed.
   */
  public static int[] getOrCreateHitsMask(String className, int length) {
    if (ourProjectData != null) {
      return ourProjectData.getOrCreateClass(className, length).hits;
    }
    try {
      try {
        // Here we use system class loader here as in offline instrumentation mode
        // coverage agent is not included to the bootstrap class loader
        // but added to the class path as usual jar
        return (int[]) GET_OR_CREATE_HITS_MASK_INTERNAL_METHOD.invokeStatic(RawProjectInit.class.getName(), new Object[]{className, length}, ClassLoader.getSystemClassLoader());
      } catch (ClassNotFoundException ignored) {
        // On Android devices system classloader does not contain the application classes,
        // so we use any class loaded in this case.
        return getOrCreateHitsInternal(className, length);
      }
    } catch (Exception e) {
      ErrorReporter.error("Error in class data access: " + className, e);
      return null;
    }
  }


  /**
   * This method is used in case of offline instrumentation.
   * As ProjectData is uninitialized, this method also creates ProjectData when needed and sets up reporting hook.
   */
  public static int[] getOrCreateHitsInternal(String className, int length) {
    if (ourProjectData == null) {
      synchronized (RawProjectData.class) {
        if (ourProjectData == null) {
          ourProjectData = new RawProjectData();
          final String filePath = System.getProperty("coverage.offline.report.path");
          if (filePath != null) {
            final File file = new File(filePath);
            RawHitsReport.dumpOnExit(file, ourProjectData);
            ErrorReporter.suggestBasePath(file.getParent());
          }
          try {
            // initialize Kover Runtime
            Class.forName(KOVER_INITIALIZER_CLASS_NAME);
          } catch (ClassNotFoundException e) {
            // no Kover Runtime detected
          }
        }
      }
    }
    return ourProjectData.getOrCreateClass(className, length).hits;
  }

  public static RawProjectData getProjectData() {
    RawProjectData projectData = ourProjectData;
    if (projectData == null) {
      ClassLoader loader = RawProjectData.class.getClassLoader();
      ErrorReporter.error("Coverage data is null in RawProjectInit. " +
          "This can be caused by accessing coverage data before tests' start. " +
          "Alternatively, this could be a class loading issue: the data access method is called in class loaded by "
          + loader);
    }
    return projectData;
  }
}
