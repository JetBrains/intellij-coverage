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

package com.intellij.rt.coverage.util;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public abstract class TestTrackingCallback {
  private final File myDataFile;
  private File myTracesDir;

  /**
   * Test tracking trace storage. Test tracking supports only sequential tests (but code inside one test could be parallel).
   * Nevertheless, in case of parallel tests run setting storage to null truncates coverage significantly.
   * Using CAS for the storage update slightly improves test tracking coverage as the data are not cleared too frequently.
   */
  private final AtomicReference<Map<Object, boolean[]>> myTrace = new AtomicReference<Map<Object, boolean[]>>();

  protected TestTrackingCallback(File file) {
    myDataFile = file;
  }

  public abstract void clearTrace(ClassData classData);
  public abstract boolean[] traceLine(ClassData classData, int line);

  private Map<Object, boolean[]> getTraces() {
    return myTrace.get();
  }

  public void traceLineByTest(Object classData, int line) {
    final Map<Object, boolean[]> traces = getTraces();
    if (traces != null) {
      final boolean[] lines = traceLine((ClassData) classData, line);
      if (lines != null) {
        traces.put(classData, lines);
      }
    }
  }

  public void registerForTrace(Object classData) {
    final Map<Object, boolean[]> traces = getTraces();
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
        clearTrace(classData);
      }
    }
  }

  /**
   * This method could be called in test tracking mode by test engine listeners
   */
  public void testStarted(final String ignoredName) {
    myTrace.compareAndSet(null, new ConcurrentHashMap<Object, boolean[]>());
  }

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
