package com.intellij.rt.coverage.data;


import com.intellij.rt.coverage.instrumentation.ClassFinder;
import com.intellij.rt.coverage.util.CoverageIOUtil;
import com.intellij.rt.coverage.util.SourceLineCounter;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;
import gnu.trove.TIntObjectProcedure;
import gnu.trove.TObjectIntHashMap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.EmptyVisitor;

import java.io.*;
import java.util.*;

public class ProjectData implements CoverageData, Serializable {
  public static final String PROJECT_DATA_OWNER = "com/intellij/rt/coverage/data/ProjectData";
  public static final String PROJECT_DATA_TYPE = "Lcom/intellij/rt/coverage/data/ProjectData;";

  public static ProjectData ourProjectData;

  private File myDataFile;
  private final Map myClasses = new HashMap(1000);
  private final TObjectIntHashMap myDict = new TObjectIntHashMap();

  private String myCurrentTestName;

  private boolean myTraceLines;
  private Map myTrace;
  private List myIncludePatterns;
  private List myExcludePatterns;
  private boolean myAppendUnloaded;

  public ClassData getClassData(final String name) {
    return (ClassData)myClasses.get(name);
  }

  public ClassData createClassData(String name) {
    final ClassData classData = new ClassData(name);
    myClasses.put(name, classData);
    return classData;
  }

  public static ProjectData getProjectData() {
    return ourProjectData;
  }

  public static ProjectData createProjectData(final File dataFile, final boolean traceLines, boolean calcUnloaded, boolean mergeWithExisting) throws IOException {
    ourProjectData = new ProjectData();
    ourProjectData.myAppendUnloaded = calcUnloaded;
    ourProjectData.myTraceLines = traceLines;
    ourProjectData.myDataFile = dataFile;
    if (!dataFile.exists()) {
      final File parentDir = dataFile.getParentFile();
      if (parentDir != null && !parentDir.exists()) parentDir.mkdirs();
      dataFile.createNewFile();
    } else if (mergeWithExisting) {
      ourProjectData.merge(ProjectDataLoader.load(dataFile));
    }
    if (traceLines) new TIntHashSet();//instrument TIntHashSet
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      public void run() {
        if (ourProjectData.myAppendUnloaded) {
          appendUnloaded();
        }

        DataOutputStream os = null;
        try {
          os = new DataOutputStream(new FileOutputStream(ourProjectData.myDataFile));
          ourProjectData.save(os);
        }
        catch (IOException e) {
          System.err.println("coverage agent: error writing file " + dataFile.getPath() + ": " + e.getMessage());
        }
        finally {
          try {
            if (os != null) {
              os.close();
            }
          }
          catch (IOException e) {
            System.err.println("coverage agent: error writing file " + dataFile.getPath() + ": " + e.getMessage());
          }
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


  public void testEnded(final String name) {
    if (myTrace == null) return;
    final String dirName = getNameWithoutExtension(myDataFile.getName());
    final File dir = new File(myDataFile.getParent(), dirName);
    final File traceFile = new File(dir, name + ".tr");
    try {
      if (!dir.exists()) {
        dir.mkdirs();
      }
      if (!traceFile.exists()) {
        traceFile.createNewFile();
      }
      DataOutputStream os = null;
      try {
        os = new DataOutputStream(new FileOutputStream(traceFile));
        os.writeInt(myTrace.size());
        for (Iterator it = myTrace.keySet().iterator(); it.hasNext();) {
          final String className = (String)it.next();
          os.writeUTF(className);
          final TIntHashSet lines = (TIntHashSet)myTrace.get(className);
          os.writeInt(lines.size());
          for(TIntIterator linesIt = lines.iterator(); linesIt.hasNext();) {
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
      System.err.println("coverage agent: error writing traces to file " + traceFile.getPath() + ": " + e.getMessage());
    }
    finally {
      myTrace = null;
    }
  }

  public String getCurrentTestName() {
    return myCurrentTestName;
  }

  public void testStarted(final String name) {
    myCurrentTestName = name;
    if (myTraceLines) myTrace = new HashMap();
  }

  public void trace(String classFQName, int line) {
    if (myTrace != null) {
      synchronized (myTrace) {
        TIntHashSet lines = (TIntHashSet)myTrace.get(classFQName);
        if (lines == null) {
          lines = new TIntHashSet();
          myTrace.put(classFQName, lines);
        }
        lines.add(line);
      }
    }
  }

  //copypaste due to 1.3 compatibility
  public static String getNameWithoutExtension(String name) {
    int i = name.lastIndexOf('.');
    if (i != -1) {
      name = name.substring(0, i);
    }
    return name;
  }


  public Map getClasses() {
    return myClasses;
  }

  public static int getDictValue(String className) {
    return ourProjectData.myDict.get(className);
  }

  public void setIncludePatterns(List includePatterns) {
    myIncludePatterns = includePatterns;
  }

  public void setExcludePatterns(List excludePatterns) {
    myExcludePatterns = excludePatterns;
  }

   private static void appendUnloaded() {
    Collection matchedClasses = new ClassFinder(ourProjectData.myIncludePatterns, ourProjectData.myExcludePatterns).findMatchedClasses();

    for (Iterator matchedClassIterator = matchedClasses.iterator(); matchedClassIterator.hasNext();) {
      String className = (String)matchedClassIterator.next();
      ClassData cd = ourProjectData.getClassData(className);
      if (cd != null) continue;
      try {
        ClassReader reader = new ClassReader(className);
        SourceLineCounter slc = new SourceLineCounter(new EmptyVisitor(), cd, ourProjectData.myTraceLines);
        reader.accept(slc, 0);
        if (slc.getNSourceLines() > 0) { // ignore classes without executable code
          final ClassData classData = ourProjectData.createClassData(className);
          slc.getSourceLines().forEachEntry(new TIntObjectProcedure() {
            public boolean execute(int line, Object methodSig) {
              final LineData ld = classData.addLine(line, (String)methodSig);
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
}
