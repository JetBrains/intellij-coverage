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

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.dataAccess.*;
import com.intellij.rt.coverage.instrumentation.testTracking.TestTrackingMode;
import com.intellij.rt.coverage.util.ClassNameUtil;
import com.intellij.rt.coverage.util.OptionsUtil;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.regex.Pattern;

public class CoverageTransformer extends AbstractIntellijClassfileTransformer {

  private final ProjectData data;
  private final boolean shouldSaveSource;
  private final List<Pattern> excludePatterns;
  private final List<Pattern> includePatterns;
  private final ClassFinder cf;
  private final TestTrackingMode testTrackingMode;

  public CoverageTransformer(ProjectData data, boolean shouldSaveSource, List<Pattern> excludePatterns, List<Pattern> includePatterns) {
    this(data, shouldSaveSource, excludePatterns, includePatterns, null, null);
  }

  public CoverageTransformer(ProjectData data, boolean shouldSaveSource, List<Pattern> excludePatterns, List<Pattern> includePatterns, ClassFinder cf, TestTrackingMode testTrackingMode) {
    this.data = data;
    this.shouldSaveSource = shouldSaveSource;
    this.excludePatterns = excludePatterns;
    this.includePatterns = includePatterns;
    this.cf = cf;
    this.testTrackingMode = testTrackingMode;
  }

  @Override
  protected ClassVisitor createClassVisitor(String className, ClassLoader loader, ClassReader cr, ClassVisitor cw) {
    return InstrumentationStrategy.createInstrumenter(data, className, cr, cw, testTrackingMode, data.isBranchCoverage(),
        shouldSaveSource, createDataAccess(className, cr, data.isBranchCoverage()));
  }

  private CoverageDataAccess createDataAccess(String className, ClassReader cr, boolean branchCoverage) {
    if (!branchCoverage && OptionsUtil.NEW_LINE_COVERAGE_ENABLED || branchCoverage && OptionsUtil.NEW_BRANCH_COVERAGE_ENABLED) {
      if (OptionsUtil.CONDY_ENABLED && InstrumentationUtils.getBytecodeVersion(cr) >= Opcodes.V11) {
        return new CondyCoverageDataAccess(createCondyInit(className, cr));
      } else {
        return new FieldCoverageDataAccess(cr, className, createInit(className, cr));
      }
    } else {
      return new NameCoverageDataAccess(createInit(className, cr));
    }
  }

  protected CoverageDataAccess.Init createInit(String className, ClassReader cr) {
    return new CoverageDataAccess.Init("__$hits$__", DataAccessUtil.HITS_ARRAY_TYPE, ProjectData.PROJECT_DATA_OWNER,
        "getHitsMask", "(Ljava/lang/String;)" + DataAccessUtil.HITS_ARRAY_TYPE, new Object[]{className});
  }

  protected CoverageDataAccess.Init createCondyInit(String className, ClassReader cr) {
    return new CoverageDataAccess.Init("__$hits$__", DataAccessUtil.HITS_ARRAY_TYPE, "com/intellij/rt/coverage/util/CondyUtils",
        "getHitsMask", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;)" + DataAccessUtil.HITS_ARRAY_TYPE, new Object[]{className});
  }

  @Override
  protected boolean shouldExclude(String className) {
    return ClassNameUtil.matchesPatterns(className, excludePatterns);
  }

  @Override
  protected InclusionPattern getInclusionPattern() {
    return includePatterns.isEmpty() ? null : new InclusionPattern() {
      public boolean accept(String className) {
        return ClassNameUtil.matchesPatterns(className, includePatterns);
      }
    };
  }

  @Override
  protected void visitClassLoader(ClassLoader classLoader) {
    if (cf != null) {
      cf.addClassLoader(classLoader);
    }
  }

  @Override
  protected boolean isStopped() {
    return data.isStopped();
  }
}
