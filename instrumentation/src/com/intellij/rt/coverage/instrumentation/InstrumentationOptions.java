/*
 * Copyright 2000-2024 JetBrains s.r.o.
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

import com.intellij.rt.coverage.instrumentation.testTracking.TestTrackingMode;
import com.intellij.rt.coverage.util.OptionsUtil;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class InstrumentationOptions {
  public static final InstrumentationOptions DEFAULT = new InstrumentationOptions.Builder().build();

  public final boolean isBranchCoverage;
  public final boolean isMergeData;
  public final boolean isCalculateUnloaded;
  public final boolean isInstructionCoverage;
  public final boolean isCalculateHits;
  public final boolean isSaveSource;

  public final List<Pattern> includePatterns;
  public final List<Pattern> excludePatterns;
  public final List<Pattern> includeAnnotations;
  public final List<Pattern> excludeAnnotations;

  public final File dataFile;
  public final File sourceMapFile;

  public final TestTrackingMode testTrackingMode;

  public InstrumentationOptions(
      boolean isBranchCoverage, boolean isMergeData, boolean isCalculateUnloaded, boolean isInstructionCoverage,
      boolean isCalculateHits, boolean isSaveSource,
      List<Pattern> includePatterns, List<Pattern> excludePatterns,
      List<Pattern> includeAnnotations, List<Pattern> excludeAnnotations,
      File dataFile, File sourceMapFile,
      TestTrackingMode testTrackingMode) {
    this.isBranchCoverage = isBranchCoverage;
    this.isMergeData = isMergeData;
    this.isCalculateUnloaded = isCalculateUnloaded;
    this.isInstructionCoverage = isInstructionCoverage;
    this.isCalculateHits = isCalculateHits;
    this.isSaveSource = isSaveSource;
    this.includePatterns = includePatterns;
    this.excludePatterns = excludePatterns;
    this.includeAnnotations = includeAnnotations;
    this.excludeAnnotations = excludeAnnotations;
    this.dataFile = dataFile;
    this.sourceMapFile = sourceMapFile;
    this.testTrackingMode = testTrackingMode;
  }

  public boolean isTestTracking() {
    return testTrackingMode != null;
  }

  public static class Builder {
    private boolean isBranchCoverage = true;
    private boolean isMergeData = false;
    private boolean isCalculateUnloaded = false;
    private boolean isInstructionCoverage = OptionsUtil.INSTRUCTIONS_COVERAGE_ENABLED;
    private boolean isCalculateHits = OptionsUtil.CALCULATE_HITS_COUNT;
    private boolean isSaveSource = false;
    private List<Pattern> includePatterns = Collections.emptyList();
    private List<Pattern> excludePatterns = Collections.emptyList();
    private List<Pattern> includeAnnotations = Collections.emptyList();
    private List<Pattern> excludeAnnotations = Collections.emptyList();
    private File dataFile = null;
    private File sourceMapFile = null;
    private TestTrackingMode testTrackingMode = null;

    public Builder setBranchCoverage(boolean isBranchCoverage) {
      this.isBranchCoverage = isBranchCoverage;
      return this;
    }

    public Builder setIsMergeData(boolean isMergeData) {
      this.isMergeData = isMergeData;
      return this;
    }

    public Builder setIsCalculateUnloaded(boolean isCalculateUnloaded) {
      this.isCalculateUnloaded = isCalculateUnloaded;
      return this;
    }

    public Builder setInstructionCoverage(boolean isInstructionCoverage) {
      this.isInstructionCoverage = isInstructionCoverage;
      return this;
    }

    public Builder setIsCalculateHits(boolean isCalculateHits) {
      this.isCalculateHits = isCalculateHits;
      return this;
    }

    public Builder setSaveSource(boolean isSaveSource) {
      this.isSaveSource = isSaveSource;
      return this;
    }

    public Builder setIncludePatterns(List<Pattern> includePatterns) {
      this.includePatterns = includePatterns;
      return this;
    }

    public Builder setExcludePatterns(List<Pattern> excludePatterns) {
      this.excludePatterns = excludePatterns;
      return this;
    }

    public Builder setIncludeAnnotations(List<Pattern> includeAnnotations) {
      this.includeAnnotations = includeAnnotations;
      return this;
    }

    public Builder setExcludeAnnotations(List<Pattern> excludeAnnotations) {
      this.excludeAnnotations = excludeAnnotations;
      return this;
    }

    public Builder setDataFile(File dataFile) {
      this.dataFile = dataFile;
      return this;
    }

    public Builder setSourceMapFile(File sourceMapFile) {
      this.sourceMapFile = sourceMapFile;
      return this;
    }

    public Builder setTestTrackingMode(TestTrackingMode testTrackingMode) {
      this.testTrackingMode = testTrackingMode;
      return this;
    }

    public InstrumentationOptions build() {
      return new InstrumentationOptions(
          isBranchCoverage, isMergeData, isCalculateUnloaded, isInstructionCoverage, isCalculateHits,
          isSaveSource || sourceMapFile != null,
          includePatterns, excludePatterns, includeAnnotations, excludeAnnotations,
          dataFile, sourceMapFile, testTrackingMode);
    }
  }
}
