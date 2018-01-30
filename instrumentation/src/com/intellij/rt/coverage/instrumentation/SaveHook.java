/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import com.intellij.rt.coverage.data.LineCoverage;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.util.*;
import com.intellij.rt.coverage.util.classFinder.ClassEntry;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntObjectProcedure;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.org.objectweb.asm.ClassReader;

import java.io.*;
import java.util.*;

/**
 * @author anna
 * @since 26-Feb-2010
 */
public class SaveHook implements Runnable {
    private final File myDataFile;
    private File mySourceMapFile;
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
        projectData.stop();
        try {
            if (myAppendUnloaded) {
                appendUnloaded(projectData);
            }

            DataOutputStream os = null;
            try {
                os = CoverageIOUtil.openFile(myDataFile);
                projectData.checkLineMappings();
                final TObjectIntHashMap dict = new TObjectIntHashMap();
                final Map classes = new HashMap(projectData.getClasses());
                CoverageIOUtil.writeINT(os, classes.size());
                saveDictionary(os, dict, classes);
                saveData(os, dict, classes);

                saveSourceMap(classes, mySourceMapFile);
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
            ErrorReporter.reportError("Out of memory error occurred, try to increase memory available for the JVM, or make include / exclude patterns more specific", e);
        } catch (Throwable e) {
            ErrorReporter.reportError("Unexpected error", e);
        }
    }

    public static void saveSourceMap(Map str_clData_classes, File sourceMapFile) {
        if (sourceMapFile != null) {
            Map readNames = Collections.emptyMap();
            try {
                if (sourceMapFile.exists()) {
                    readNames = loadSourceMapFromFile(str_clData_classes, sourceMapFile);
                }
            } catch (IOException e) {
                ErrorReporter.reportError("Error loading source map from " + sourceMapFile.getPath(), e);
            }

            try {
                doSaveSourceMap(readNames, sourceMapFile, str_clData_classes);
            } catch (IOException e) {
                ErrorReporter.reportError("Error writing source map " + sourceMapFile.getPath(), e);
            }
        }
    }

    public static Map loadSourceMapFromFile(Map classes, File mySourceMapFile) throws IOException {
        DataInputStream in = null;
        try {
            in = new DataInputStream(new FileInputStream(mySourceMapFile));
            final int classNumber = CoverageIOUtil.readINT(in);
            final HashMap readNames = new HashMap(classNumber);
            for (int i = 0; i < classNumber; ++i) {
                final String className = CoverageIOUtil.readUTFFast(in);
                final String classSource = CoverageIOUtil.readUTFFast(in);
                if ("".equals(classSource)) {
                    continue;
                }
                ClassData data = (ClassData) classes.get(className);
                if (data == null) {
                    readNames.put(className, classSource);
                } else if (data.getSource() == null || !data.getSource().equals(classSource)) {
                    readNames.put(className, classSource);
                }
            }
            return readNames;
        } finally { if (in != null) in.close(); }
    }

    private static void saveData(DataOutputStream os, final TObjectIntHashMap dict, Map classes) throws IOException {
      for (Object o : classes.values()) {
        ((ClassData) o).save(os, new DictionaryLookup() {
          public int getDictionaryIndex(String className) {
            return dict.containsKey(className) ? dict.get(className) : -1;
          }
        });
      }
    }

    private static void saveDictionary(DataOutputStream os, TObjectIntHashMap dict, Map classes) throws IOException {
        int i = 0;
      for (Object o : classes.keySet()) {
        String className = (String) o;
        dict.put(className, i++);
        CoverageIOUtil.writeUTF(os, className);
      }
    }

    public static void doSaveSourceMap(Map str_str_readNames, File sourceMapFile, Map str_clData_classes) throws IOException {
        HashMap str_str_merged_map = new HashMap(str_str_readNames);
      for (Object o1 : str_clData_classes.values()) {
        ClassData classData = ((ClassData) o1);
        if (!str_str_merged_map.containsKey(classData.getName())) {
          str_str_merged_map.put(classData.getName(), classData.getSource());
        }
      }

        DataOutputStream out = null;
        try {
            out = CoverageIOUtil.openFile(sourceMapFile);
            CoverageIOUtil.writeINT(out, str_str_merged_map.size());
          for (Object o : str_str_merged_map.entrySet()) {
            Map.Entry str_str_entry = (Map.Entry) o;
            CoverageIOUtil.writeUTF(out, (String) str_str_entry.getKey());
            final String value = (String) str_str_entry.getValue();
            CoverageIOUtil.writeUTF(out, value != null ? value : "");
          }
        } finally {
            if (out != null) CoverageIOUtil.close(out);
        }
    }

    private void appendUnloaded(final ProjectData projectData) {

        Collection matchedClasses = myClassFinder.findMatchedClasses();

      for (Object matchedClass : matchedClasses) {
        ClassEntry classEntry = (ClassEntry) matchedClass;
        ClassData cd = projectData.getClassData(classEntry.getClassName());
        if (cd != null) continue;
        try {
          ClassReader reader = new ClassReader(classEntry.getClassInputStream());
          if (mySourceMapFile != null && cd == null) {
            cd = projectData.getOrCreateClassData(classEntry.getClassName());
          }
          SourceLineCounter slc = new SourceLineCounter(cd, !projectData.isSampling(), mySourceMapFile != null ? projectData : null);
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
          e.printStackTrace();
          ErrorReporter.reportError("Failed to process class: " + classEntry.getClassName() + ", error: " + e.getMessage(), e);
        }
      }
    }

    public void setSourceMapFile(File sourceMapFile) {
        mySourceMapFile = sourceMapFile;
    }
}