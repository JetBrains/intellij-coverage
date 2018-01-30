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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TestDiscoveryProjectData {
  public static final String PROJECT_DATA_OWNER = "com/intellij/rt/coverage/data/TestDiscoveryProjectData";
  public static final String TEST_DISCOVERY_DATA_LISTENER_PROP = "test.discovery.data.listener";

  protected static TestDiscoveryProjectData ourProjectData = new TestDiscoveryProjectData();

  public TestDiscoveryProjectData() {
    try {
      myDataListener = (TestDiscoveryDataListener) Class.forName(System.getProperty(TEST_DISCOVERY_DATA_LISTENER_PROP, TrFileDiscoveryDataListener.class.getName())).newInstance();
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
          myDataListener.testsFinished();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }));
  }

  public static TestDiscoveryProjectData getProjectData() {
    return ourProjectData;
  }

  private final ConcurrentMap<String, boolean[]> myClassToVisitedMethods = new ConcurrentHashMap<String, boolean[]>();
  private final ConcurrentMap<String, String[]> myClassToMethodNames = new ConcurrentHashMap<String, String[]>();
  private final TestDiscoveryDataListener myDataListener;

  // called from instrumented code during class's static init
  public static boolean[] trace(String className, boolean[] methodFlags, String[] methodNames) {
    return ourProjectData.traceLines(className, methodFlags, methodNames);
  }

  private synchronized boolean[] traceLines(String className, boolean[] methodFlags, String[] methodNames) {
    //System.out.println("Registering " + className);
    //assert methodFlags.length == methodNames.length;
    final boolean[] previousMethodFlags = myClassToVisitedMethods.putIfAbsent(className, methodFlags);

    if (previousMethodFlags != null) {
      //  assert previousMethodFlags.length == methodFlags.length;
      final String[] previousMethodNames = myClassToMethodNames.get(className);
      //assert previousMethodNames != null && previousMethodNames.length == methodNames.length;
    } else {
      myClassToMethodNames.put(className, methodNames);
    }
    return previousMethodFlags != null ? previousMethodFlags : methodFlags;
  }

  public synchronized void testDiscoveryEnded(final String name) {
    try {
      myDataListener.testFinished(name, myClassToVisitedMethods, myClassToMethodNames);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public synchronized void testDiscoveryStarted(final String name) {
    for (Object e : myClassToVisitedMethods.entrySet()) {
      boolean[] used = (boolean[]) ((Map.Entry) e).getValue();
      for (int i = 0, len = used.length; i < len; ++i) {
        if (used[i]) used[i] = false;
      }
    }
  }

}
