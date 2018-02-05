/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TestDiscoveryProjectData {
  public static final String PROJECT_DATA_OWNER = "com/intellij/rt/coverage/data/TestDiscoveryProjectData";
  public static final String TEST_DISCOVERY_DATA_LISTENER_PROP = "test.discovery.data.listener";

  protected static final TestDiscoveryProjectData ourProjectData = new TestDiscoveryProjectData();
  private final NameEnumerator myNameEnumerator;

  private TestDiscoveryProjectData() {
    try {
      String testDiscoveryDataListener = System.getProperty(TEST_DISCOVERY_DATA_LISTENER_PROP);
      if (testDiscoveryDataListener == null) {
        throw new RuntimeException("Property \"" + TEST_DISCOVERY_DATA_LISTENER_PROP + "\" should be specified");
      }
      myDataListener = (TestDiscoveryDataListener) Class.forName(testDiscoveryDataListener).newInstance();
      myNameEnumerator = myDataListener.getNameEnumerator();
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }

    //TODO do via event
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      public void run() {
        try {
          testDiscoveryFinished();
          double allTraceTime = ourTraceTime / (1000 * 1000 * 1000.0);
          double allCleanupTime = ourCleanupTime / (1000 * 1000 * 1000.0);
          System.out.println("Trace time: " + allTraceTime + ", cleanup: " + allCleanupTime);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }));
  }

  public static TestDiscoveryProjectData getProjectData() {
    return ourProjectData;
  }

  private final ConcurrentMap<Integer, boolean[]> myClassToVisitedMethods = new ConcurrentHashMap<Integer, boolean[]>();
  private final ConcurrentMap<Integer, int[]> myClassToMethodNames = new ConcurrentHashMap<Integer, int[]>();
  final ConcurrentMap<Integer, ClassMetadata> classesToMetadata = new ConcurrentHashMap<Integer, ClassMetadata>();
  private final TestDiscoveryDataListener myDataListener;

  // called from instrumented code during class's static init
  public static boolean[] trace(String className, boolean[] methodFlags, String[] methodNames) {
    long s = System.nanoTime();
    try {
      return ourProjectData.traceLines(className, methodFlags, methodNames);
    } finally {
      ourTraceTime += System.nanoTime() - s;
    }
  }

  private static Long ourTraceTime = 0L;
  private static Long ourCleanupTime = 0L;

  private synchronized boolean[] traceLines(String className, boolean[] methodFlags, String[] methodNames) {
    //System.out.println("Registering " + className);
    //assert methodFlags.length == methodNames.length;
    int classId = myNameEnumerator.enumerate(className);

    final boolean[] previousMethodFlags = myClassToVisitedMethods.putIfAbsent(classId, methodFlags);

    if (previousMethodFlags != null) {
      if (previousMethodFlags.length == methodFlags.length) {
        return previousMethodFlags;
      }
      //override previous data so different loaded classes would work with different arrays 
      //the last loaded class wins but at least no ArrayIndexOutOfBound would be possible due to different class versions
      myClassToVisitedMethods.put(classId, methodFlags);
    }

    myClassToMethodNames.put(classId, NameEnumerator.enumerate(methodNames, myNameEnumerator));
    return methodFlags;
  }

  public synchronized void testDiscoveryEnded(final String className, final String methodName) {
    try {
      myDataListener.testFinished(className, methodName, myClassToVisitedMethods, myClassToMethodNames);
      for (Map.Entry<Integer, boolean[]> e : myClassToVisitedMethods.entrySet()) {
        for (boolean isUsed : e.getValue()) {
          if (isUsed) {
            ClassMetadata cm = classesToMetadata.remove(e.getKey());
            if (cm != null) {
              myDataListener.addClassMetadata(Collections.singletonList(cm));
            }
            break;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public synchronized void testDiscoveryStarted(final String className, final String methodName) {
    long s = System.nanoTime();
    try {
      cleanup();
    } finally {
      ourTraceTime += System.nanoTime() - s;
    }
  }

  private void cleanup() {
    for (Object e : myClassToVisitedMethods.entrySet()) {
      boolean[] used = (boolean[]) ((Map.Entry) e).getValue();
      for (int i = 0, len = used.length; i < len; ++i) {
        if (used[i]) used[i] = false;
      }
    }
  }

  private volatile boolean myFinished;

  private synchronized void testDiscoveryFinished() {
    if (myFinished) return;
    myFinished = true;
    try {
      myDataListener.testsFinished();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void addClassMetadata(List<ClassMetadata> classMetadata) {
    for (ClassMetadata cm : classMetadata) {
      classesToMetadata.put(myNameEnumerator.enumerate(cm.getFqn()), cm);
    }
  }

  //TestOnly
  NameEnumerator getMyNameEnumerator() {
    return myNameEnumerator;
  }

  //TestOnly
  ConcurrentMap<Integer, int[]> getClassToMethodNames() {
    return myClassToMethodNames;
  }

  //TestOnly
  ConcurrentMap<Integer, boolean[]> getClassToVisitedMethods() {
    return myClassToVisitedMethods;
  }
}
