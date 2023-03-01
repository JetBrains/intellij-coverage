/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

package com.intellij.rt.coverage.report;

import java.util.*;

public class XMLProjectData {
  private final Map<String, FileInfo> myFiles = new HashMap<String, FileInfo>();
  private final Map<String, ClassInfo> myClasses = new HashMap<String, ClassInfo>();

  public void addClass(ClassInfo info) {
    myClasses.put(info.name, info);
  }

  public void addFile(FileInfo info) {
    myFiles.put(info.path, info);
  }

  public ClassInfo getClass(String name) {
    return myClasses.get(name);
  }

  public FileInfo getFile(String path) {
    return myFiles.get(path);
  }

  public Collection<ClassInfo> getClasses() {
    return myClasses.values();
  }

  public Collection<FileInfo> getFiles() {
    return myFiles.values();
  }

  public static class ClassInfo {
    public final String name;
    public final String fileName;
    public final int missedLines;
    public final int coveredLines;
    public final int missedInstructions;
    public final int coveredInstructions;
    public final int missedBranches;
    public final int coveredBranches;
    public final int missedMethods;
    public final int coveredMethods;

    public ClassInfo(String name, String fileName,
                     int missedLines, int coveredLines, int missedInstructions, int coveredInstructions,
                     int missedBranches, int coveredBranches, int missedMethods, int coveredMethods) {
      this.name = name;
      this.fileName = fileName;
      this.missedLines = missedLines;
      this.coveredLines = coveredLines;
      this.missedInstructions = missedInstructions;
      this.coveredInstructions = coveredInstructions;
      this.missedBranches = missedBranches;
      this.coveredBranches = coveredBranches;
      this.missedMethods = missedMethods;
      this.coveredMethods = coveredMethods;
    }
  }

  public static class FileInfo {
    public final String path;
    public final List<LineInfo> lines = new ArrayList<LineInfo>();

    public FileInfo(String path) {
      this.path = path;
    }
  }

  public static class LineInfo {
    public final int lineNumber;
    public int missedInstructions;
    public int coveredInstructions;
    public int missedBranches;
    public int coveredBranches;

    public LineInfo(int lineNumber, int missedInstructions, int coveredInstructions, int missedBranches, int coveredBranches) {
      this.lineNumber = lineNumber;
      this.missedInstructions = missedInstructions;
      this.coveredInstructions = coveredInstructions;
      this.missedBranches = missedBranches;
      this.coveredBranches = coveredBranches;
    }

    public LineInfo(int lineNumber) {
      this.lineNumber = lineNumber;
    }
  }
}
