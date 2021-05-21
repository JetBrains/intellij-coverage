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

package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.FileMapData;
import com.intellij.rt.coverage.data.LineMapData;
import com.intellij.rt.coverage.util.ClassNameUtil;
import org.jetbrains.coverage.gnu.trove.THashSet;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.gnu.trove.TObjectFunction;

import java.util.*;

/**
 * @author anna
 * @since 2/8/11
 */
public class JSR45Util {
  private static final String FILE_SECTION = "*F\n";
  private static final String LINE_SECTION = "*L\n";
  private static final String SECTION_SEPARATOR = "*";

  private static final LineMapData[] EMPTY_LINE_MAP = new LineMapData[0];

  private static String checkSMAP(String debug) {
    return debug.startsWith("SMAP") ? debug.substring(4) : null;
  }

  public static FileMapData[] extractLineMapping(String debug, String className) {
    debug = checkSMAP(debug);
    if (debug != null) {
      final TIntObjectHashMap<THashSet<LineMapData>> linesMap = new TIntObjectHashMap<THashSet<LineMapData>>();
      final int fileSectionIdx = debug.indexOf(FILE_SECTION);
      final int lineInfoIdx = debug.indexOf(LINE_SECTION);
      final TIntObjectHashMap<String> fileNames = parseFileNames(debug, fileSectionIdx, lineInfoIdx, className);
      final int lineInfoStart = lineInfoIdx + LINE_SECTION.length();
      final int lineInfoEnd = debug.indexOf(SECTION_SEPARATOR, lineInfoStart);
      final String lineInfo = debug.substring(lineInfoStart, lineInfoEnd);
      final String[] lines = lineInfo.split("\n");
      int fileId = 1;
      for (String line : lines) {
        //InputStartLine # LineFileID , RepeatCount : OutputStartLine , OutputLineIncrement
        int startSrcLine;
        int repeat = 1;
        int startOutLine;
        int outLineInc = 1;

        final int idx = line.indexOf(":");
        final String srcLine = line.substring(0, idx);
        final String outLine = line.substring(idx + 1);

        final int srcCommaIdx = srcLine.indexOf(',');
        final int sharpIdx = srcLine.indexOf("#");
        if (sharpIdx > -1) {
          startSrcLine = Integer.parseInt(srcLine.substring(0, sharpIdx));
          if (srcCommaIdx > -1) {
            repeat = Integer.parseInt(srcLine.substring(srcCommaIdx + 1));
            fileId = Integer.parseInt(srcLine.substring(sharpIdx + 1, srcCommaIdx));
          } else {
            fileId = Integer.parseInt(srcLine.substring(sharpIdx + 1));
          }
        } else if (srcCommaIdx > -1) {
          repeat = Integer.parseInt(srcLine.substring(srcCommaIdx + 1));
          startSrcLine = Integer.parseInt(srcLine.substring(0, srcCommaIdx));
        } else {
          startSrcLine = Integer.parseInt(srcLine);
        }

        final int outCommaIdx = outLine.indexOf(',');
        if (outCommaIdx > -1) {
          outLineInc = Integer.parseInt(outLine.substring(outCommaIdx + 1));
          startOutLine = Integer.parseInt(outLine.substring(0, outCommaIdx));
        } else {
          startOutLine = Integer.parseInt(outLine);
        }

        THashSet<LineMapData> currentFile = linesMap.get(fileId);
        if (currentFile == null) {
          currentFile = new THashSet<LineMapData>();
          linesMap.put(fileId, currentFile);
        }
        for (int r = 0; r < repeat; r++) {
          currentFile.add(new LineMapData(startSrcLine + r, startOutLine + r * outLineInc, startOutLine + (r + 1) * outLineInc - 1));
        }
      }

      final List<FileMapData> result = new ArrayList<FileMapData>();
      final int[] keys = linesMap.keys();
      Arrays.sort(keys);
      for (final int key : keys) {
        result.add(new FileMapData(fileNames.get(key), getLinesMapping(linesMap.get(key))));
      }
      return result.toArray(FileMapData.EMPTY_FILE_MAP);
    }
    return null;
  }

  private static String[] getFileSectionLines(String debug, int fileSectionIdx, int lineInfoIdx) {
    String fileSection = debug.substring(fileSectionIdx + FILE_SECTION.length(), lineInfoIdx);
    fileSection = fileSection.trim();
    if (fileSection.endsWith("\n")) {
      fileSection = fileSection.substring(0, fileSection.length() - 1);
    }
    return fileSection.split("\n");
  }

  private static TIntObjectHashMap<String> parseFileNames(String debug, int fileSectionIdx, int lineInfoIdx, String className) {
    final String defaultPrefix = getClassPackageName(className);
    final String[] fileNameIdx = getFileSectionLines(debug, fileSectionIdx, lineInfoIdx);
    final TIntObjectHashMap<String> result = new TIntObjectHashMap<String>();
    boolean generatedPrefix = true;
    for (int i = 0; i < fileNameIdx.length; i++) {
      String fileName = fileNameIdx[i];
      String idAndName = fileName;
      String path = null;
      if (fileName.startsWith("+ ")) {
        idAndName = fileName.substring(2);
        path = fileNameIdx[++i];
      }
      int idx = idAndName.indexOf(" ");
      int key = Integer.parseInt(idAndName.substring(0, idx));
      String currentClassName = idAndName.substring(idx + 1);

      path = path == null ? currentClassName : processRelative(path);
      final int lastDot = path.lastIndexOf(".");
      String fileNameWithDots;
      if (lastDot < 0) {
        fileNameWithDots = path;
      } else {
        fileNameWithDots = path.substring(0, lastDot) + "_" + path.substring(lastDot + 1);
      }
      fileNameWithDots = ClassNameUtil.convertToFQName(fileNameWithDots);
      
      generatedPrefix &= !fileNameWithDots.startsWith(defaultPrefix);
      currentClassName = fileNameWithDots;
      result.put(key, currentClassName);
    }

    if (generatedPrefix) {
      result.transformValues(new TObjectFunction<String, String>() {
        public String execute(String selfValue) {
          return defaultPrefix + selfValue;
        }
      });
    }
    return result;
  }

  public static String processRelative(String fileName) {
    int idx;
    while ((idx = fileName.indexOf("..")) > -1) {
      final String rest = fileName.substring(idx + "..".length());
      String start = fileName.substring(0, idx);
      if (!start.endsWith("/")) return fileName;
      start = start.substring(0, start.length() - 1);
      final int endIndex = start.lastIndexOf('/');
      if (endIndex > -1) {
        fileName = start.substring(0, endIndex) + rest;
      }
      else {
        fileName = rest.startsWith("/") ? rest.substring(1) : rest;
      }
    }
    return fileName;
  }

  public static String getClassPackageName(String className) {
    String generatePrefix = "";
    final int fqnLastDotIdx = className.lastIndexOf(".");
    if (fqnLastDotIdx > -1) {
      generatePrefix = className.substring(0, fqnLastDotIdx + 1);
    }
    return generatePrefix;
  }

  private static LineMapData[] getLinesMapping(THashSet<LineMapData> linesMap) {
    final LineMapData[] result = linesMap.toArray(EMPTY_LINE_MAP);
    Arrays.sort(result, new Comparator<LineMapData>() {
      public int compare(LineMapData o1, LineMapData o2) {
        int compareSource = o1.getSourceLineNumber() - o2.getSourceLineNumber();
        if (compareSource == 0) {
          int compareMin = o1.getTargetMinLine() - o2.getTargetMinLine();
          if (compareMin == 0) {
            return o1.getTargetMaxLine() - o2.getTargetMaxLine();
          }
          return compareMin;
        }
        return compareSource;
      }
    });
    return result;
  }

  /**
   * @param debug SourceDebugExtension of .class-file
   * @return list of paths to source files .class-file was generated from, empty list if none are found
   */
  public static List<String> parseSourcePaths(String debug) {
    debug = checkSMAP(debug);
    if (debug != null) {
      String[] fileNameIdx = getFileSectionLines(debug, debug.indexOf(FILE_SECTION), debug.indexOf(LINE_SECTION));
      List<String> paths = new ArrayList<String>();
      for (int i = 0; i < fileNameIdx.length; i++) {
        String fileName = fileNameIdx[i];
        String idAndName = fileName;
        String path = null;
        if (fileName.startsWith("+ ")) {
          idAndName = fileName.substring(2);
          path = fileNameIdx[++i];
        }
        int idx = idAndName.indexOf(" ");
        String currentClassName = idAndName.substring(idx + 1);
        if (path == null) {
          path = currentClassName;
        }
        else {
          path = processRelative(path);
          int lastSlashIdx = path.lastIndexOf("/");
          if (lastSlashIdx > 0) {
            path = path.substring(0, ++lastSlashIdx) + currentClassName;
          }
        }
        paths.add(path);
      }
      return paths;
    }
    return Collections.emptyList();
  }
}
