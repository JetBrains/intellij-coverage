/*
 * User: anna
 * Date: 26-Feb-2010
 */
package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineCoverage;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.util.StringsPool;
import com.intellij.rt.coverage.util.CoverageIOUtil;
import com.intellij.rt.coverage.util.DictionaryLookup;
import com.intellij.rt.coverage.util.ErrorReporter;
import gnu.trove.TIntObjectProcedure;
import gnu.trove.TObjectIntHashMap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.commons.EmptyVisitor;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SaverHook implements Runnable {
  private final File myDataFile;
  private final boolean myAppendUnloaded;
  private final ClassFinder myClassFinder;

  public SaverHook(File dataFile, boolean appendUnloaded, ClassFinder classFinder) {
    myDataFile = dataFile;
    myAppendUnloaded = appendUnloaded;
    this.myClassFinder = classFinder;
  }

  public void run() {
    try {
      if (myAppendUnloaded) {
        appendUnloaded();
      }

      final ProjectData projectData = ProjectData.getProjectData();
      DataOutputStream os = null;
      try {
        os = new DataOutputStream(new FileOutputStream(myDataFile));
        final TObjectIntHashMap dict = new TObjectIntHashMap();
        int i = 0;
        final Map classes = new HashMap(projectData.getClasses());
        CoverageIOUtil.writeINT(os, classes.size());
        for (Iterator it = classes.keySet().iterator(); it.hasNext();) {
          String className = (String) it.next();
          dict.put(className, i++);
          CoverageIOUtil.writeUTF(os, className);
        }
        projectData.save(os, new DictionaryLookup() {
          public int getDictionaryIndex(String className) {
            return dict.get(className);
          }
        });
      }
      catch (IOException e) {
        ErrorReporter.reportError("Error writing file " + myDataFile.getPath(), e);
      }
      finally {
        try {
          if (os != null) {
            os.close();
          }
        }
        catch (IOException e) {
          ErrorReporter.reportError("Error writing file " + myDataFile.getPath(), e);
        }
      }
    } catch (OutOfMemoryError e) {
      ErrorReporter.reportError("Out of memory error occured, try to increase memory available for the JVM, or make include / exclude patterns more specific", e);
    } catch (Throwable e) {
      ErrorReporter.reportError("Unexpected error", e);
    }
  }

  private void appendUnloaded() {

    Collection matchedClasses = myClassFinder.findMatchedClasses();

    for (Iterator matchedClassIterator = matchedClasses.iterator(); matchedClassIterator.hasNext();) {
      ClassEntry classEntry = (ClassEntry) matchedClassIterator.next();
      ClassData cd = ProjectData.getProjectData().getClassData(classEntry.getClassName());
      if (cd != null) continue;
      try {
        ClassReader reader = new ClassReader(classEntry.getClassInputStream());
        SourceLineCounter slc = new SourceLineCounter(new EmptyVisitor(), cd, !ProjectData.getProjectData().isSampling());
        reader.accept(slc, 0);
        if (slc.getNSourceLines() > 0) { // ignore classes without executable code
          final ClassData classData = ProjectData.getProjectData().getOrCreateClassData(StringsPool.getFromPool(classEntry.getClassName()));
          slc.getSourceLines().forEachEntry(new TIntObjectProcedure() {
            public boolean execute(int line, Object methodSig) {
              final LineData ld = classData.getOrCreateLine(line, StringsPool.getFromPool((String)methodSig));
              classData.registerMethodSignature(ld);
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