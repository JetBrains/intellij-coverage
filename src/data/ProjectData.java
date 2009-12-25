package com.intellij.rt.coverage.data;


import com.intellij.rt.coverage.instrumentation.ClassEntry;
import com.intellij.rt.coverage.instrumentation.ClassFinder;
import com.intellij.rt.coverage.util.*;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;
import gnu.trove.TIntObjectProcedure;
import gnu.trove.TObjectIntHashMap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.EmptyVisitor;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ProjectData implements CoverageData, Serializable {
  public static final String PROJECT_DATA_OWNER = "com/intellij/rt/coverage/data/ProjectData";
  private static final Class[] TRACE_LINE_PARAMS = new Class[]{String.class, int.class};
  private static final Class[] TOUCH_JUMP_PARAMS = new Class[] {int.class, int.class, boolean.class};
  private static final Class[] TOUCH_SWITCH_PARAMS = new Class[] {int.class, int.class, int.class};
  private static final Class[] TOUCH_LINE_PARAMS = new Class[] {int.class};


  public static ProjectData ourProjectData;

  private File myDataFile;
  private final Map myClasses = new HashMap(1000);
  private final TObjectIntHashMap myDict = new TObjectIntHashMap();

  private String myCurrentTestName;

  private boolean myTraceLines;
  private boolean mySampling;
  private Map myTrace;
  private ClassFinder myClassFinder;
  private boolean myAppendUnloaded;
  private File myTracesDir;
  private static Method ourGetClassMethod;
  private static Object ourProjectDataObject;
  private static Method ourTraceLineMethod;
  private static Map ourTouchMethods;

  public ClassData getClassData(final String name) {
    return (ClassData)myClasses.get(name);
  }

  public ClassData getOrCreateClassData(String name) {
    ClassData classData = (ClassData) myClasses.get(name);
    if (classData == null) {
      String reusedName = StringsPool.getFromPool(name);
      classData = new ClassData(reusedName);
      myClasses.put(reusedName, classData);
    }
    return classData;
  }

  public void setClassFinder(ClassFinder cf) {
    myClassFinder = cf;
  }

  public static ProjectData getProjectData() {
    return ourProjectData;
  }

  public boolean isSampling() {
    return mySampling;
  }

  public static ProjectData createProjectData(final File dataFile, final boolean traceLines, boolean calcUnloaded, boolean mergeWithExisting, boolean isSampling) throws IOException {
    ourProjectData = new ProjectData();
    if (!dataFile.exists()) {
      final File parentDir = dataFile.getParentFile();
      if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();
      dataFile.createNewFile();
    } else if (mergeWithExisting) {
      ourProjectData = ProjectDataLoader.load(dataFile);
    }
    ourProjectData.myAppendUnloaded = calcUnloaded;
    ourProjectData.mySampling = isSampling;
    ourProjectData.myTraceLines = traceLines;
    ourProjectData.myDataFile = dataFile;
    if (traceLines) new TIntHashSet();//instrument TIntHashSet
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      public void run() {
        try {
          if (ourProjectData.myAppendUnloaded) {
            appendUnloaded();
          }

          DataOutputStream os = null;
          try {
            os = new DataOutputStream(new FileOutputStream(ourProjectData.myDataFile));
            ourProjectData.save(os);
          }
          catch (IOException e) {
            ErrorReporter.reportError("Error writing file " + dataFile.getPath(), e);
          }
          finally {
            try {
              if (os != null) {
                os.close();
              }
            }
            catch (IOException e) {
              ErrorReporter.reportError("Error writing file " + dataFile.getPath(), e);
            }
          }
        } catch (OutOfMemoryError e) {
          ErrorReporter.reportError("Out of memory error occured, try to increase memory available for the JVM, or make include / exclude patterns more specific", e);
        } catch (Throwable e) {
          ErrorReporter.reportError("Unexpected error", e);
        }
      }
    }));
    return ourProjectData;
  }

  public void save(DataOutputStream os) throws IOException {
    final HashMap classes = new HashMap(myClasses);
    CoverageIOUtil.writeINT(os, classes.size());
    initAndSaveDict(os, classes);
    for (Iterator it = classes.values().iterator(); it.hasNext();) {
      ((ClassData)it.next()).save(os);
    }
  }

  private void initAndSaveDict(DataOutputStream os, HashMap classes) throws IOException {
    int i = 0;
    for (Iterator it = classes.keySet().iterator(); it.hasNext();) {
      String className = (String)it.next();
      myDict.put(className, i++);
      CoverageIOUtil.writeUTF(os, className);
    }
  }

  public void merge(final CoverageData data) {
    final ProjectData projectData = (ProjectData)data;
    for (Iterator iter = projectData.myClasses.keySet().iterator(); iter.hasNext();) {
      final Object key = iter.next();
      final ClassData mergedData = (ClassData)projectData.myClasses.get(key);
      final ClassData classData = (ClassData)myClasses.get(key);
      if (classData != null) {
        classData.merge(mergedData);
      }
      else {
        myClasses.put(key, mergedData);
      }
    }
  }

 // --------------- used from listeners --------------------- //
 public void testEnded(final String name) {
   if (myTrace == null) return;
   final File traceFile = new File(getTracesDir(), name + ".tr");
   try {
     if (!traceFile.exists()) {
       traceFile.createNewFile();
     }
     DataOutputStream os = null;
     try {
       os = new DataOutputStream(new FileOutputStream(traceFile));
       os.writeInt(myTrace.size());
       for (Iterator it = myTrace.keySet().iterator(); it.hasNext();) {
         final String className = (String) it.next();
         os.writeUTF(className);
         final TIntHashSet lines = (TIntHashSet) myTrace.get(className);
         os.writeInt(lines.size());
         for (TIntIterator linesIt = lines.iterator(); linesIt.hasNext();) {
           os.writeInt(linesIt.next());
         }
       }
     }
     finally {
       if (os != null) {
         os.close();
       }
     }
   }
   catch (IOException e) {
     ErrorReporter.reportError("Error writing traces to file " + traceFile.getPath(), e);
   }
   finally {
     myTrace = null;
   }
 }

  public void testStarted(final String name) {
    myCurrentTestName = name;
    if (myTraceLines) myTrace = new HashMap();
  }
  //---------------------------------------------------------- //


  private File getTracesDir() {
    if (myTracesDir == null) {
      final String fileName = myDataFile.getName();
      final int i = fileName.lastIndexOf('.');
      final String dirName = i != -1 ? fileName.substring(0, i) : fileName;
      myTracesDir = new File(myDataFile.getParent(), dirName);
      if (!myTracesDir.exists()) {
        myTracesDir.mkdirs();
      }
    }
    return myTracesDir;
  }

  public String getCurrentTestName() {
    return myCurrentTestName;
  }


  public static int getDictValue(String className) {
    return ourProjectData.myDict.get(className);
  }

   private static void appendUnloaded() {
    if (ourProjectData.myClassFinder == null) {
        System.err.println("ClassFinder is not set");
        return;
    }

    Collection matchedClasses = ourProjectData.myClassFinder.findMatchedClasses();

    for (Iterator matchedClassIterator = matchedClasses.iterator(); matchedClassIterator.hasNext();) {
      ClassEntry classEntry = (ClassEntry) matchedClassIterator.next();
      ClassData cd = ourProjectData.getClassData(classEntry.getClassName());
      if (cd != null) continue;
      try {
        ClassReader reader = new ClassReader(classEntry.getClassInputStream());
        SourceLineCounter slc = new SourceLineCounter(new EmptyVisitor(), cd, !ourProjectData.isSampling());
        reader.accept(slc, 0);
        if (slc.getNSourceLines() > 0) { // ignore classes without executable code
          final ClassData classData = ourProjectData.getOrCreateClassData(classEntry.getClassName());
          slc.getSourceLines().forEachEntry(new TIntObjectProcedure() {
            public boolean execute(int line, Object methodSig) {
              final LineData ld = classData.getOrCreateLine(line, (String)methodSig);
              ld.setStatus(LineCoverage.NONE);
              return true;
            }
          });
        }
      }
      catch (IOException e) {
        // failed to read class
      }
    }
  }


  // -----------------------  used from instrumentation  ------------------------------------------------//

  //load ProjectData always through system class loader (null) then user's ClassLoaders won't affect    //
  //IMPORTANT: do not remove reflection, it was introduced to avoid ClassCastExceptions in CoverageData //
  //loaded via user's class loader                                                                      //

  // -------------------------------------------------------------------------------------------------- //

  public static void touchLine(String className, int line) {
    touch("touchLine",
          className,
          TOUCH_LINE_PARAMS,
          new Object[]{new Integer(line)});
  }

  public static void touchSwitch(String className, int line, int switchNumber, int key) {
    if (ourProjectData != null) {
      ourProjectData.getClassData(className).touch(line, switchNumber, key);
      return;
    }
    touch("touch",
          className,
          TOUCH_SWITCH_PARAMS,
          new Object[]{new Integer(line), new Integer(switchNumber), new Integer(key)});
  }

  public static void touchJump(String className, int line, int jump, boolean hit) {
    if (ourProjectData != null) {
      ourProjectData.getClassData(className).touch(line, jump, hit);
      return;
    }
    touch("touch",
          className,
          TOUCH_JUMP_PARAMS,
          new Object[]{new Integer(line), new Integer(jump), new Boolean(hit)});
  }

  public static void trace(String classFQName, int line) {
    if (ourProjectData != null) {
      ourProjectData.getClassData(classFQName).touch(line);
      ourProjectData.traceLine(classFQName, line);
      return;
    }

    touch("touch",
          classFQName,
          TOUCH_LINE_PARAMS,
          new Object[]{new Integer(line)});
    try {
      final Object projectData = getProjectDataObject();
      if (ourTraceLineMethod == null) {
        ourTraceLineMethod = projectData.getClass().getDeclaredMethod("traceLine", TRACE_LINE_PARAMS);
      }
      ourTraceLineMethod.invoke(projectData, new Object[]{classFQName,  new Integer(line)});
    } catch (Exception e) {
      ErrorReporter.reportError("Error tracing class " + classFQName, e);
    }
  }

  private static void touch(final String touchMethodName, String className, final Class[] paramTypes, final Object[] paramValues) {
    try {
      final Object projectDataObject = getProjectDataObject();
      if (ourGetClassMethod == null) {
        ourGetClassMethod = projectDataObject.getClass().getMethod("getClassData", new Class[]{String.class});
      }
      final Object classData = ourGetClassMethod.invoke(projectDataObject, new Object[]{className});
      final Class classDataClass = classData.getClass();
      final Method touchMethod = getTouchMethod(touchMethodName, paramTypes, classDataClass);
      touchMethod.invoke(classData, paramValues);
    } catch (Exception e) {
      ErrorReporter.reportError("Error in project data collection: " + touchMethodName, e);
    }
  }

  private static Method getTouchMethod(String touchMethodName, Class[] paramTypes, Class classDataClass) throws NoSuchMethodException {
    if (ourTouchMethods == null) ourTouchMethods = new HashMap();
    Object touchMethod = ourTouchMethods.get(paramTypes);
    if (touchMethod == null) {
      touchMethod = classDataClass.getDeclaredMethod(touchMethodName, paramTypes);
      ourTouchMethods.put(paramTypes, touchMethod);
    }
    return (Method) touchMethod;
  }

  private static Object getProjectDataObject() throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException {
    if (ourProjectDataObject == null) {
      final Class projectDataClass = Class.forName(ProjectData.class.getName(), false, null);
      ourProjectDataObject = projectDataClass.getDeclaredField("ourProjectData").get(null);
    }
    return ourProjectDataObject;
  }

  public void traceLine(String classFQName, int line) {
    if (myTrace != null) {
      synchronized (myTrace) {
        TIntHashSet lines = (TIntHashSet) myTrace.get(classFQName);
        if (lines == null) {
          lines = new TIntHashSet();
          myTrace.put(classFQName, lines);
        }
        lines.add(line);
      }
    }
  }
  // ----------------------------------------------------------------------------------------------- //
}
