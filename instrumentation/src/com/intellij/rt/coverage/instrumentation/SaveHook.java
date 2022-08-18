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

import com.intellij.rt.coverage.data.*;
import com.intellij.rt.coverage.data.instructions.InstructionsUtil;
import com.intellij.rt.coverage.instrumentation.filters.classFilter.PrivateConstructorOfUtilClassFilter;
import com.intellij.rt.coverage.instrumentation.filters.visiting.KotlinInlineVisitingFilter;
import com.intellij.rt.coverage.util.*;
import com.intellij.rt.coverage.util.classFinder.ClassEntry;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.gnu.trove.TIntObjectProcedure;
import org.jetbrains.coverage.gnu.trove.TObjectIntHashMap;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

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
    private final boolean myMergeFile;

    public SaveHook(File dataFile, boolean appendUnloaded, ClassFinder classFinder, boolean mergeFile) {
        myDataFile = dataFile;
        myAppendUnloaded = appendUnloaded;
        myClassFinder = classFinder;
        myMergeFile = mergeFile;
    }

    public void run() {
        save(ProjectData.getProjectData());
    }

    public void save(ProjectData projectData) {
        projectData.stop();
        CoverageIOUtil.FileLock lock = null;
        try {
            projectData.applyLinesMask();
            projectData.applyBranchData();
            if (myAppendUnloaded) {
              final boolean calculateSource = mySourceMapFile != null;
              if (OptionsUtil.UNLOADED_CLASSES_FULL_ANALYSIS) {
                  appendUnloadedFullAnalysis(projectData, myClassFinder, calculateSource, projectData.isSampling(), OptionsUtil.IGNORE_PRIVATE_CONSTRUCTOR_OF_UTIL_CLASS, false);
                } else {
                  appendUnloaded(projectData, myClassFinder, calculateSource, projectData.isSampling());
                }
            }
            projectData.checkLineMappings();
            dropIgnoredLines(projectData);
            checkLineSignatures(projectData);
            lock = CoverageIOUtil.FileLock.lock(myDataFile);
            if (myMergeFile) {
                final ProjectData load = ProjectDataLoader.load(myDataFile);
                projectData.merge(load);
            }

            save(projectData, myDataFile, mySourceMapFile);
        } catch (OutOfMemoryError e) {
            ErrorReporter.reportError("Out of memory error occurred, try to increase memory available for the JVM, or make include / exclude patterns more specific", e);
        } catch (Throwable e) {
            ErrorReporter.reportError("Unexpected error", e);
        } finally {
            CoverageIOUtil.FileLock.unlock(lock);
        }
    }

    public static void save(ProjectData projectData, File dataFile, File sourceMapFile) {
        DataOutputStream os = null;
        try {
            os = CoverageIOUtil.openFile(dataFile);
            final TObjectIntHashMap<String> dict = new TObjectIntHashMap<String>();
            final Map<String, ClassData> classes = new HashMap<String, ClassData>(projectData.getClasses());
            CoverageIOUtil.writeINT(os, classes.size());
            saveDictionary(os, dict, classes);
            saveData(os, dict, classes);

            CoverageIOUtil.writeINT(os, ProjectDataLoader.REPORT_VERSION);
            CoverageIOUtil.writeUTF(os, getExtraInfoString());
            ReportSectionsUtil.saveSections(projectData, os, dict);

            saveSourceMap(classes, sourceMapFile);
        } catch (IOException e) {
            ErrorReporter.reportError("Error writing file " + dataFile.getPath(), e);
        } finally {
            CoverageIOUtil.close(os);
        }
    }

    /**
     * This line may contain some useful configuration for sections parsing.
     * This field is string type to be extended easily.If a new agent version relies on this line data,
     * it must be extended such that it is possible to parse it and use for further extensions.
     */
    private static String getExtraInfoString() {
        return "";
    }

    public static void saveSourceMap(Map str_clData_classes, File sourceMapFile) {
        if (sourceMapFile != null) {
            Map<Object, Object> readNames = Collections.emptyMap();
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

    public static void loadAndApplySourceMap(ProjectData projectData, File sourceMapFile) throws IOException {
        Map<Object, Object> map = loadSourceMapFromFile(new HashMap(), sourceMapFile);
        for (Object o : map.entrySet()) {
            @SuppressWarnings("unchecked") Map.Entry<String, String> entry = (Map.Entry<String, String>) o;
            String className = entry.getKey();
            String source = entry.getValue();
            ClassData data = projectData.getClassData(className);
            if (data != null) {
              data.setSource(source);
            }
        }
    }

    public static Map<Object, Object> loadSourceMapFromFile(Map classes, File mySourceMapFile) throws IOException {
        DataInputStream in = null;
        try {
            in = new DataInputStream(new FileInputStream(mySourceMapFile));
            final int classNumber = CoverageIOUtil.readINT(in);
            final HashMap<Object, Object> readNames = new HashMap<Object, Object>(classNumber);
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
        } finally { CoverageIOUtil.close(in); }
    }

    private static void saveData(DataOutputStream os, final TObjectIntHashMap<String> dict, Map classes) throws IOException {
      for (Object o : classes.values()) {
        ((ClassData) o).save(os, new DictionaryLookup() {
          public int getDictionaryIndex(String className) {
            return dict.containsKey(className) ? dict.get(className) : -1;
          }
        });
      }
    }

    private static void saveDictionary(DataOutputStream os, TObjectIntHashMap<String> dict, Map classes) throws IOException {
        int i = 0;
      for (Object o : classes.keySet()) {
        String className = (String) o;
        dict.put(className, i++);
        CoverageIOUtil.writeUTF(os, className);
      }
    }

    public static void doSaveSourceMap(Map<Object, Object> str_str_readNames, File sourceMapFile, Map str_clData_classes) throws IOException {
        HashMap<Object, Object> str_str_merged_map = new HashMap<Object, Object>(str_str_readNames);
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
            CoverageIOUtil.close(out);
        }
    }

  /**
   * Append classes that had not been loaded during the program run into the <code>projectData</code>.
   *
   * Classes are searched using <code>classFinder</code>.
   */
    public static void appendUnloaded(final ProjectData projectData, final ClassFinder classFinder, final boolean calculateSource, final boolean isSampling) {
      classFinder.iterateMatchedClasses(new ClassEntry.Consumer() {
        public void consume(ClassEntry classEntry) {
          ClassData cd = projectData.getClassData(StringsPool.getFromPool(classEntry.getClassName()));
          if (cd != null && cd.getLines() != null) return;
          try {
            final InputStream classInputStream = classEntry.getClassInputStream();
            if (classInputStream == null) return;
            ClassReader reader = new ClassReader(classInputStream);
            if (calculateSource) {
              cd = projectData.getOrCreateClassData(StringsPool.getFromPool(classEntry.getClassName()));
            }
            SourceLineCounter slc = new SourceLineCounter(cd, calculateSource ? projectData : null, !isSampling);
            reader.accept(slc, ClassReader.SKIP_FRAMES);
            if (slc.isEnum() || slc.getNSourceLines() > 0) { // ignore classes without executable code
              final TIntObjectHashMap<LineData> lines = new TIntObjectHashMap<LineData>(4, 0.99f);
              final int[] maxLine = new int[]{-1};
              final ClassData classData = projectData.getOrCreateClassData(StringsPool.getFromPool(classEntry.getClassName()));
              slc.getSourceLines().forEachEntry(new TIntObjectProcedure<String>() {
                public boolean execute(int line, String methodSig) {
                  final LineData ld = new LineData(line, StringsPool.getFromPool(methodSig));
                  lines.put(line, ld);
                  if (line > maxLine[0]) maxLine[0] = line;
                  classData.registerMethodSignature(ld);
                  ld.setStatus(LineCoverage.NONE);
                  return true;
                }
              });
              final TIntObjectHashMap<JumpsAndSwitches> jumpsPerLine = slc.getJumpsPerLine();
              if (jumpsPerLine != null) {
                jumpsPerLine.forEachEntry(new TIntObjectProcedure<JumpsAndSwitches>() {
                  public boolean execute(int line, JumpsAndSwitches jumpData) {
                    final LineData lineData = lines.get(line);
                    if (lineData != null) {
                      lineData.setJumpsAndSwitches(jumpData);
                      lineData.fillArrays();
                    }
                    return true;
                  }
                });
              }
              classData.setLines(LinesUtil.calcLineArray(maxLine[0], lines));
            }
          } catch (Throwable e) {
            ErrorReporter.reportError("Failed to process unloaded class: " + classEntry.getClassName() + ", error: " + e.getMessage(), e);
          }
        }
      });
    }

  public static final MethodVisitor EMPTY_METHOD_VISITOR = new MethodVisitor(Opcodes.API_VERSION) {};
  public static final ClassVisitor EMPTY_CLASS_VISITOR = new ClassVisitor(Opcodes.API_VERSION) {
    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
      return EMPTY_METHOD_VISITOR;
    }
  };

  public static void appendUnloadedFullAnalysis(final ProjectData projectData, final ClassFinder classFinder, final boolean calculateSource, final boolean isSampling, final boolean ignorePrivateConstructorOfUtilClass) {
    appendUnloadedFullAnalysis(projectData, classFinder, calculateSource, isSampling, ignorePrivateConstructorOfUtilClass, true);
  }


  public static void appendUnloadedFullAnalysis(final ProjectData projectData, final ClassFinder classFinder,
                                                final boolean calculateSource, final boolean isSampling,
                                                final boolean ignorePrivateConstructorOfUtilClass,
                                                final boolean checkLineMappings) {
    classFinder.iterateMatchedClasses(new ClassEntry.Consumer() {
      public void consume(ClassEntry classEntry) {
        final ClassData cd = projectData.getClassData(StringsPool.getFromPool(classEntry.getClassName()));
        if (cd != null && cd.getLines() != null) return;
        try {
          final InputStream is = classEntry.getClassInputStream();
          if (is == null) return;
          appendUnloadedClass(projectData, classEntry.getClassName(), new ClassReader(is), isSampling, calculateSource, ignorePrivateConstructorOfUtilClass, checkLineMappings);
        } catch (Throwable e) {
          ErrorReporter.reportError("Failed to process unloaded class: " + classEntry.getClassName() + ", error: " + e.getMessage(), e);
        }
      }
    });
  }

  @SuppressWarnings("unused") // used in IntelliJ
  public static void appendUnloadedClass(ProjectData projectData, String className, ClassReader reader, boolean isSampling, boolean calculateSource, boolean ignorePrivateConstructorOfUtilClass) {
    appendUnloadedClass(projectData, className, reader, isSampling, calculateSource, ignorePrivateConstructorOfUtilClass, true);
  }

  private static void appendUnloadedClass(ProjectData projectData, String className, ClassReader reader, boolean isSampling, boolean calculateSource, boolean ignorePrivateConstructorOfUtilClass, boolean checkLineMappings) {
    final Instrumenter instrumenter;
    if (isSampling) {
      instrumenter = new SamplingInstrumenter(projectData, EMPTY_CLASS_VISITOR, className, calculateSource);
    } else {
      instrumenter = new TracingInstrumenter(projectData, EMPTY_CLASS_VISITOR, className, calculateSource);
    }
    ClassVisitor visitor = instrumenter;
    if (ignorePrivateConstructorOfUtilClass) {
      visitor = PrivateConstructorOfUtilClassFilter.createWithContext(visitor, instrumenter);
    }
    reader.accept(visitor, ClassReader.SKIP_FRAMES);
    final ClassData classData = projectData.getClassData(className);
    if (classData == null || classData.getLines() == null) return;
    classData.dropIgnoredLines();
    final LineData[] lines = (LineData[]) classData.getLines();
    for (LineData line : lines) {
      if (line == null) continue;
      classData.registerMethodSignature(line);
    }
    if (!checkLineMappings) return;
    final Map<String, FileMapData[]> linesMap = projectData.getLinesMap();
    if (linesMap == null) return;
    final FileMapData[] mappings = linesMap.remove(className);
    if (mappings == null) return;
    classData.dropMappedLines(mappings);
    InstructionsUtil.dropMappedLines(projectData, classData.getName(), mappings);
  }

    public void setSourceMapFile(File sourceMapFile) {
        mySourceMapFile = sourceMapFile;
    }

    private void checkLineSignatures(ProjectData projectData) {
      if (!KotlinInlineVisitingFilter.shouldCheckLineSignatures()) return;
      final Map<String, FileMapData[]> linesMap = projectData.getLinesMap();
      if (linesMap == null) return;
      final Set<String> classes = new HashSet<String>();
      for (Map.Entry<String, FileMapData[]> mapData : linesMap.entrySet()) {
        if (mapData.getValue() == null) continue;
        for (FileMapData data : mapData.getValue()) {
          if (data == null) continue;
          if (mapData.getKey().equals(data.getClassName())) continue;
          classes.add(data.getClassName());
        }
      }
      for (String className : classes) {
        final ClassData classData = projectData.getClassData(className);
        if (classData == null) continue;
        KotlinInlineVisitingFilter.checkLineSignatures(classData, myClassFinder);
      }
    }

    private void dropIgnoredLines(ProjectData projectData) {
      for (final ClassData classData : projectData.getClassesCollection()) {
        classData.dropIgnoredLines();
      }
    }
}