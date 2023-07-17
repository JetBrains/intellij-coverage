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

package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.dataAccess.CoverageDataAccess;
import com.intellij.rt.coverage.instrumentation.filters.FilterUtils;
import com.intellij.rt.coverage.instrumentation.filters.classFilter.ClassFilter;
import com.intellij.rt.coverage.instrumentation.filters.classes.ClassSignatureFilter;
import com.intellij.rt.coverage.instrumentation.testTracking.TestTrackingMode;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;

import java.util.List;

public class InstrumentationStrategy {
  private static final List<ClassSignatureFilter> ourFilters = FilterUtils.createClassSignatureFilters();

  /**
   * Create instrumenter for class or return null if class should be ignored.
   */
  static ClassVisitor createInstrumenter(ProjectData data, String className,
                                         ClassReader cr, ClassVisitor cw, TestTrackingMode testTrackingMode,
                                         boolean branchCoverage,
                                         boolean shouldSaveSource,
                                         CoverageDataAccess dataAccess) {
    // uncomment to get readable bytecode
    // cw = new TraceClassVisitor(cw, new PrintWriter(System.err));

    for (ClassSignatureFilter filter : ourFilters) {
      if (filter.shouldFilter(cr)) return null;
    }
    final Instrumenter instrumenter;
    if (branchCoverage) {
      if (testTrackingMode != null) {
        instrumenter = testTrackingMode.createInstrumenter(data, cw, cr, className, shouldSaveSource, dataAccess);
      } else {
        instrumenter = new BranchesInstrumenter(data, cw, className, shouldSaveSource, dataAccess);
      }
    } else {
      instrumenter = new LineInstrumenter(data, cw, className, shouldSaveSource, dataAccess);
    }
    ClassVisitor result = instrumenter;
    for (ClassFilter cv : FilterUtils.createClassFilters()) {
      if (cv.isApplicable(instrumenter)) {
        cv.initFilter(instrumenter, result);
        result = cv;
      }
    }
    return result;
  }
}
