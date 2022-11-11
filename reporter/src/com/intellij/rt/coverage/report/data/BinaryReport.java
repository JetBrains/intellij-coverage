/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package com.intellij.rt.coverage.report.data;

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.CoverageReport;
import com.intellij.rt.coverage.util.ErrorReporter;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import com.intellij.rt.coverage.util.RawHitsReport;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class BinaryReport {
  private final File myDataFile;
  private final File mySourceMapFile;
  private byte myIsRawReport = 2;

  public BinaryReport(File dataFile, @Nullable File sourceMapFile) {
    myDataFile = dataFile;
    mySourceMapFile = sourceMapFile;
  }

  public File getDataFile() {
    return myDataFile;
  }

  public boolean isRawHitsReport() {
    if (myIsRawReport <= 1) {
      return myIsRawReport == 1;
    }
    boolean isRaw = false;
    try {
      isRaw = RawHitsReport.isRawHitsFile(myDataFile);
    } catch (IOException e) {
      ErrorReporter.reportError("Failed to check raw report file", e);
    }
    myIsRawReport = isRaw ? (byte) 1 : (byte) 0;
    return isRaw;
  }

  @Nullable
  public File getSourceMapFile() {
    return mySourceMapFile;
  }

  public ProjectData loadData() {
    final ProjectData data = ProjectDataLoader.load(myDataFile);
    if (mySourceMapFile != null) {
      try {
        CoverageReport.loadAndApplySourceMap(data, mySourceMapFile);
      } catch (IOException e) {
        ErrorReporter.reportError("Error in processing source map file", e);
      }
    }
    return data;
  }
}
