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
import gnu.trove.THashSet;
import gnu.trove.TIntObjectHashMap;

import java.util.Iterator;

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
      final TIntObjectHashMap linesMap = new TIntObjectHashMap();
      debug = debug.substring(4);
      final int fileSectionIdx = debug.indexOf(FILE_SECTION);
      final int lineInfoIdx = debug.indexOf(LINE_SECTION);
      final String[] fileNames = parseFileNames(debug.substring(fileSectionIdx + FILE_SECTION.length(), lineInfoIdx), className);
      final String lineInfo = debug.substring(lineInfoIdx + LINE_SECTION.length(), debug.indexOf(END_SECTION));
      final String[] lines = lineInfo.split("\n");
      int fileId = 0;
      for (int i = 0; i < lines.length; i++) {
        //InputStartLine # LineFileID , RepeatCount : OutputStartLine , OutputLineIncrement
        int startSrcLine;
        int repeat = 1;
        int startOutLine;
        int outLineInc = 1;

        final int idx = lines[i].indexOf(":");
        final String srcLine = lines[i].substring(0, idx);
        final String outLine = lines[i].substring(idx + 1);

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

        THashSet currentFile = (THashSet) linesMap.get(fileId);
        if (currentFile == null) {
          currentFile = new THashSet();
          linesMap.put(fileId, currentFile);
        }
        for (int r = 0; r < repeat; r++) {
          currentFile.add(new LineMapData(startSrcLine + r, startOutLine + r * outLineInc, startOutLine + (r + 1) * outLineInc - 1));
        }
      }

      final FileMapData[] result = new FileMapData[linesMap.size()];
      final int[] keys = linesMap.keys();
      for (int i = 0; i < keys.length; i++) {
        final int key = keys[i];
        result[key] = new FileMapData(fileNames[key], getLinesMapping((THashSet) linesMap.get(key)));
      }
      return result;
    }
    return null;
  }

  public static String[] parseFileNames(String fileSection, String className) {

    fileSection = fileSection.trim();
    if (fileSection.endsWith("\n")) {
      fileSection = fileSection.substring(0, fileSection.length() - 1);
    }

    final String[] fileNameIdx = fileSection.split("\n");
    final String[] result = new String[fileNameIdx.length / 2];
    for (int i = 0; i < fileNameIdx.length; i++) {
      String fileName = fileNameIdx[i];
      if (fileName.startsWith("+")) continue;
      if (i / 2 == 0) {
        result[0] = className;
      } else {
        fileName = processRelative(fileName);
        final int lastDot = fileName.lastIndexOf(".");
          final String fileNameWithDots;
          if (lastDot < 0) {
            fileNameWithDots = fileName;
          }
          else {
            fileNameWithDots = fileName.substring(0, lastDot) + "_" + fileName.substring(lastDot + 1);
          }
          result[i / 2] = getClassPackageName(className) + fileNameWithDots.replace('/', '.');
      }
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

  static LineMapData[] getLinesMapping(THashSet linesMap) {

    int max = 0;
    for (Iterator iterator = linesMap.iterator(); iterator.hasNext(); ) {
      LineMapData lmd = (LineMapData) iterator.next();
      if (max < lmd.getSourceLineNumber()) {
        max = lmd.getSourceLineNumber();
      }
    }

    final LineMapData[] result = new LineMapData[max + 1];
    for (Iterator iterator = linesMap.iterator(); iterator.hasNext(); ) {
      LineMapData lmd = (LineMapData) iterator.next();
      result[lmd.getSourceLineNumber()] = lmd;
    }
    return result;
  }
}
