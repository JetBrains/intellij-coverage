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

import com.intellij.rt.coverage.data.FileMapData;
import com.intellij.rt.coverage.data.LineMapData;
import gnu.trove.*;

import java.util.*;

/**
 * @author anna
 * @since 2/8/11
 */
public class JSR45Util {
  private static final String FILE_SECTION = "*F\n";
  private static final String LINE_SECTION = "*L\n";
  private static final String END_SECTION = "*E";

  public static FileMapData[] extractLineMapping(String debug, String className) {
    if (debug.startsWith("SMAP")) {
      final TIntObjectHashMap<THashSet<LineMapData>> linesMap = new TIntObjectHashMap<THashSet<LineMapData>>();
      debug = debug.substring(4);
      final int fileSectionIdx = debug.indexOf(FILE_SECTION);
      final int lineInfoIdx = debug.indexOf(LINE_SECTION);
      final TIntObjectHashMap<String> fileNames = parseFileNames(debug.substring(fileSectionIdx + FILE_SECTION.length(), lineInfoIdx), className);
      final String lineInfo = debug.substring(lineInfoIdx + LINE_SECTION.length(), debug.indexOf(END_SECTION));
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

  private static TIntObjectHashMap<String>  parseFileNames(String fileSection, String className) {
    fileSection = fileSection.trim();
    if (fileSection.endsWith("\n")) {
      fileSection = fileSection.substring(0, fileSection.length() - 1);
    }

    final String defaultPrefix = getClassPackageName(className);
    final String[] fileNameIdx = fileSection.split("\n");
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
      fileNameWithDots = fileNameWithDots.replace('/', '.');
      
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

  private static String getClassPackageName(String className) {
    String generatePrefix = "";
    final int fqnLastDotIdx = className.lastIndexOf(".");
    if (fqnLastDotIdx > -1) {
      generatePrefix = className.substring(0, fqnLastDotIdx + 1);
    }
    return generatePrefix;
  }

  private static LineMapData[] getLinesMapping(THashSet<LineMapData> linesMap) {

    int max = 0;
    for (Object aLinesMap1 : linesMap) {
      LineMapData lmd = (LineMapData) aLinesMap1;
      if (max < lmd.getSourceLineNumber()) {
        max = lmd.getSourceLineNumber();
      }
    }

    final LineMapData[] result = new LineMapData[max + 1];
    for (Object aLinesMap : linesMap) {
      LineMapData lmd = (LineMapData) aLinesMap;
      result[lmd.getSourceLineNumber()] = lmd;
    }
    return result;
  }
}
