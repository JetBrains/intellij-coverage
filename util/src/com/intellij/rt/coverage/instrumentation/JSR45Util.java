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
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author anna
 * @since 2/8/11
 */
public class JSR45Util {
  private static final String FILE_SECTION = "*F\n";
  private static final String LINE_SECTION = "*L\n";
  private static final String SECTION_SEPARATOR = "*";

  private static final LineMapData[] EMPTY_LINE_MAP = new LineMapData[0];

  private static boolean isSmap(String debug) {
    return debug.startsWith("SMAP");
  }

  public static FileMapData[] extractLineMapping(String debug, String className) {
    if (!isSmap(debug)) return null;
    final TIntObjectHashMap<List<LineMapData>> linesMap = new TIntObjectHashMap<List<LineMapData>>();
    final int fileSectionIdx = debug.indexOf(FILE_SECTION);
    final int lineInfoIdx = debug.indexOf(LINE_SECTION);
    final List<FileInfo> fileNames = parseFileNames(debug, fileSectionIdx, lineInfoIdx, className);
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

      final int idx = line.indexOf(':');
      final String srcLine = line.substring(0, idx);
      final String outLine = line.substring(idx + 1);

      final int srcCommaIdx = srcLine.indexOf(',');
      final int sharpIdx = srcLine.indexOf('#');
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

      List<LineMapData> currentFile = linesMap.get(fileId);
      if (currentFile == null) {
        currentFile = new ArrayList<LineMapData>();
        linesMap.put(fileId, currentFile);
      }
      currentFile.add(new LineMapData(startSrcLine, repeat, startOutLine, outLineInc));
    }

    final FileMapData[] result = new FileMapData[fileNames.size()];
    for (int i = 0; i < result.length; i++) {
      final FileInfo fileInfo = fileNames.get(i);
      final List<LineMapData> mappings = linesMap.get(fileInfo.myIndex);
      final LineMapData[] array = mappings == null ? EMPTY_LINE_MAP : mappings.toArray(EMPTY_LINE_MAP);
      result[i] = new FileMapData(fileInfo.myPath, fileInfo.myName, array);
    }
    return result;
  }

  private static String[] getFileSectionLines(String debug, int fileSectionIdx, int lineInfoIdx) {
    String fileSection = debug.substring(fileSectionIdx + FILE_SECTION.length(), lineInfoIdx);
    fileSection = fileSection.trim();
    if (fileSection.endsWith("\n")) {
      fileSection = fileSection.substring(0, fileSection.length() - 1);
    }
    return fileSection.split("\n");
  }

  private static List<FileInfo> parseFileNames(String debug, int fileSectionIdx, int lineInfoIdx, String className) {
    final String defaultPrefix = getClassPackageName(className);
    final String[] fileLines = getFileSectionLines(debug, fileSectionIdx, lineInfoIdx);
    final List<FileInfo> result = new ArrayList<FileInfo>();
    boolean generatedPrefix = true;
    for (int i = 0; i < fileLines.length; i++) {
      final String fileInfoLine = fileLines[i];
      String idAndName = fileInfoLine;
      String path = null;
      if (fileInfoLine.startsWith("+ ")) {
        idAndName = fileInfoLine.substring(2);
        path = fileLines[++i];
      }
      int idx = idAndName.indexOf(" ");
      int key = Integer.parseInt(idAndName.substring(0, idx));
      final String fileName = idAndName.substring(idx + 1);

      path = path == null ? fileName : processRelative(path);
      final int lastDot = path.lastIndexOf(".");
      final String pathWithDots = ClassNameUtil.convertToFQName(lastDot < 0
          ? path
          : path.substring(0, lastDot) + "_" + path.substring(lastDot + 1));
      generatedPrefix &= !pathWithDots.startsWith(defaultPrefix);
      result.add(new FileInfo(fileName, pathWithDots, key));
    }

    if (generatedPrefix) {
      for (int i = 0; i < result.size(); i++) {
        final FileInfo fileInfo = result.get(i);
        result.set(i, new FileInfo(fileInfo.myName, defaultPrefix + fileInfo.myPath, fileInfo.myIndex));
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
      } else {
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

  /**
   * @param debug SourceDebugExtension of .class-file
   * @return list of paths to source files .class-file was generated from, empty list if none are found
   */
  public static List<String> parseSourcePaths(String debug) {
    if (!isSmap(debug)) return Collections.emptyList();
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
      } else {
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


  private static class FileInfo {
    private final String myName;
    private final String myPath;
    private final int myIndex;

    public FileInfo(String name, String path, int index) {
      myName = name;
      myPath = path;
      myIndex = index;
    }
  }
}
