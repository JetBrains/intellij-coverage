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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.intellij.rt.coverage.util.CoverageIOUtil.GIGA;

public class TestDiscoveryProjectData {
  public static final String PROJECT_DATA_OWNER = "com/intellij/rt/coverage/data/TestDiscoveryProjectData";
  public static final String TEST_DISCOVERY_DATA_LISTENER_PROP = "test.discovery.data.listener";
  public static final String INSTRUMENT_SHUTDOWN_HOOKS = "test.discovery.use.very.late.shutdown.hook";

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

    boolean useVeryLateShutDownHook = Boolean.TRUE.toString().equals(System.getProperty(INSTRUMENT_SHUTDOWN_HOOKS));
    if (useVeryLateShutDownHook && !tryRegisterHook()) {
      useVeryLateShutDownHook = false;
    }
    final boolean finalUseVeryLateShutDownHook = useVeryLateShutDownHook;
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      public void run() {
        testDiscoveryFinished();
        if (!finalUseVeryLateShutDownHook) {
          logTestInfo();
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
      myDataListener.testFinished(className, methodName, myClassToVisitedMethods, myClassToMethodNames, enumerateFiles(myOpenFilesPerTest));
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

  private List<int[]> enumerateFiles(Collection<String> openedFiles) {
    List<int[]> files = new ArrayList<int[]>(openedFiles.size());
    for (String file : openedFiles) {
      files.add(fileToInts(file));
    }
    return files;
  }

  private int[] fileToInts(String file) {
    String[] split = file.split("/");
    int[] result = new int[split.length];
    for (int i = 0; i < split.length; i++) {
      result[i] = myNameEnumerator.enumerate(split[i]);
    }
    return result;
  }

  public synchronized void testDiscoveryStarted(final String className, final String methodName) {
    long s = System.nanoTime();
    try {
      cleanup();
    } finally {
      ourCleanupTime += System.nanoTime() - s;
    }
  }

  private void cleanup() {
    for (Object e : myClassToVisitedMethods.entrySet()) {
      boolean[] used = (boolean[]) ((Map.Entry) e).getValue();
      for (int i = 0, len = used.length; i < len; ++i) {
        if (used[i]) used[i] = false;
      }
    }

    myOpenFilesPerTest.clear();
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

  private static final Map<Object, File> myOpenFilesMap = new WeakHashMap<Object, File>();
  private static final Collection<String> myOpenFilesPerTest = new LinkedHashSet<String>();

  public static final String AFFECTED_ROOTS = "test.discovery.affected.roots";
  @SuppressWarnings("WeakerAccess")
  public static final String EXCLUDED_ROOTS = "test.discovery.excluded.roots";
  @SuppressWarnings("WeakerAccess")
  public static final String TRACK_FILES = "test.discovery.track.files";

  private static final String[] myAffectedRoots = split(AFFECTED_ROOTS);
  private static final String[] myExcludedRoots = split(EXCLUDED_ROOTS);

  private static String[] split(String key) {
    String affected = System.getProperty(key);
    return affected == null ? new String[]{} : affected.split(";");
  }

  private static String stripRoot(String path) {
    for (String prefix : myAffectedRoots) {
      if (path.startsWith(prefix)) {
        return path.substring(prefix.length());
      }
    }
    return null;
  }

  private static boolean excluded(String path) {
    for (String prefix : myExcludedRoots) {
      if (path.startsWith(prefix)) return true;
    }
    return false;
  }

  private static String toSystemIndependentName(String fileName) {
    return fileName.replace('\\', '/');
  }

  public static synchronized void openPath(Object path) {
    try {
      Class<?> pathClass = Class.forName("java.nio.file.Path");
      File file = (File) pathClass.getDeclaredMethod("toFile").invoke(path);
      openFile(path, file);
    } catch (IllegalAccessException ignored) {
    } catch (InvocationTargetException ignored) {
    } catch (NoSuchMethodException ignored) {
    } catch (ClassNotFoundException ignored) {
    }
  }

  public static synchronized void openFile(Object o, File file) {
    if (file == null) return;

    String absolutePath = getPath(file);

    String trimmedPath = stripRoot(absolutePath);
    if (trimmedPath == null) return;
    if (excluded(absolutePath)) return;

    myOpenFilesMap.put(o, file);
    myOpenFilesPerTest.add(toSystemIndependentName(trimmedPath));
  }

  private static String getPath(File file) {
    try {
      return file.getCanonicalPath();
    } catch (IOException e) {
      return file.getAbsolutePath();
    }
  }

  public static synchronized void closeFile(Object o) {
    myOpenFilesMap.remove(o);
  }

  private static synchronized void logTestInfo() {
    System.out.println("Trace time: " + 1. * ourTraceTime / GIGA);
    System.out.println("Cleanup time: " + 1. * ourCleanupTime / GIGA);

    System.out.println("Leaked files: " + myOpenFilesMap.size());
    for (File value : new ArrayList<File>(myOpenFilesMap.values())) {
      System.out.println(value.getPath());
    }
  }

  private static boolean tryRegisterHook() {
    try {
      Object javaLangAccess = getJavaLangAccess();
      if (javaLangAccess == null) return false;
      Method registerShutdownHook = null;
      for (Method method : javaLangAccess.getClass().getDeclaredMethods()) {
        if (method.getName().equals("registerShutdownHook")) {
          registerShutdownHook = method;
        }
      }
      if (registerShutdownHook == null) return false;
      registerShutdownHook.setAccessible(true);
      // assume we're only one hackers
      registerShutdownHook.invoke(javaLangAccess, 9, true, new Runnable() {
        public void run() {
          TestDiscoveryProjectData.logTestInfo();
        }
      });
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private static Object getJavaLangAccess() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Class<?> sharedSecrets;
    try {
      sharedSecrets = Class.forName("sun.misc.SharedSecrets");
    } catch (ClassNotFoundException e) {
      try {
        sharedSecrets = Class.forName("jdk.internal.misc.SharedSecrets");
      } catch (ClassNotFoundException e1) {
        return null;
      }
    }
    Method getJavaLangAccessMethod = sharedSecrets.getDeclaredMethod("getJavaLangAccess");
    return getJavaLangAccessMethod.invoke(null);
  }
}
