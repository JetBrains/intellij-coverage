/*
 * User: anna
 * Date: 26-Feb-2010
 */
package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.util.*;
import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineCoverage;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.util.classFinder.ClassEntry;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;
import gnu.trove.TIntObjectHashMap;
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

public class SaveHook implements Runnable {
    private final File myDataFile;
    private final boolean myAppendUnloaded;
    private final ClassFinder myClassFinder;

    public SaveHook(File dataFile, boolean appendUnloaded, ClassFinder classFinder) {
        myDataFile = dataFile;
        myAppendUnloaded = appendUnloaded;
        myClassFinder = classFinder;
    }

    public void run() {
        save(ProjectData.getProjectData());
    }

    public void save(ProjectData projectData) {
        try {
            if (myAppendUnloaded) {
                appendUnloaded(projectData);
            }

            DataOutputStream os = null;
            try {
                os = new DataOutputStream(new FileOutputStream(myDataFile));
                final TObjectIntHashMap dict = new TObjectIntHashMap();
                final Map classes = new HashMap(projectData.getClasses());
                CoverageIOUtil.writeINT(os, classes.size());
                saveDictionary(os, dict, classes);
                projectData.save(os, new DictionaryLookup() {
                    public int getDictionaryIndex(String className) {
                        return dict.get(className);
                    }
                });
            } catch (IOException e) {
                ErrorReporter.reportError("Error writing file " + myDataFile.getPath(), e);
            } finally {
                try {
                    if (os != null) {
                        os.close();
                    }
                } catch (IOException e) {
                    ErrorReporter.reportError("Error writing file " + myDataFile.getPath(), e);
                }
            }
        } catch (OutOfMemoryError e) {
            ErrorReporter.reportError("Out of memory error occured, try to increase memory available for the JVM, or make include / exclude patterns more specific", e);
        } catch (Throwable e) {
            ErrorReporter.reportError("Unexpected error", e);
        }
    }

    private static void saveDictionary(DataOutputStream os, TObjectIntHashMap dict, Map classes) throws IOException {
        int i = 0;
        for (Iterator it = classes.keySet().iterator(); it.hasNext();) {
            String className = (String) it.next();
            dict.put(className, i++);
            CoverageIOUtil.writeUTF(os, className);
        }
    }

    private void appendUnloaded(final ProjectData projectData) {

        Collection matchedClasses = myClassFinder.findMatchedClasses();

        for (Iterator matchedClassIterator = matchedClasses.iterator(); matchedClassIterator.hasNext();) {
            ClassEntry classEntry = (ClassEntry) matchedClassIterator.next();
            ClassData cd = projectData.getClassData(classEntry.getClassName());
            if (cd != null) continue;
            try {
                ClassReader reader = new ClassReader(classEntry.getClassInputStream());
                SourceLineCounter slc = new SourceLineCounter(new EmptyVisitor(), cd, !projectData.isSampling());
                reader.accept(slc, 0);
                if (slc.getNSourceLines() > 0) { // ignore classes without executable code
                    final TIntObjectHashMap lines = new TIntObjectHashMap(4, 0.99f);
                    final int[] maxLine = new int[]{1};
                    final ClassData classData = projectData.getOrCreateClassData(StringsPool.getFromPool(classEntry.getClassName()));
                    slc.getSourceLines().forEachEntry(new TIntObjectProcedure() {
                        public boolean execute(int line, Object methodSig) {
                            final LineData ld = new LineData(line, StringsPool.getFromPool((String) methodSig));
                            lines.put(line, ld);
                            if (line > maxLine[0]) maxLine[0] = line;
                            classData.registerMethodSignature(ld);
                            ld.setStatus(LineCoverage.NONE);
                            return true;
                        }
                    });
                    classData.setLines(LinesUtil.calcLineArray(maxLine[0], lines));
                }
            } catch (Throwable e) {
                ErrorReporter.reportError(e.getMessage(), e);
            }
        }
    }
}