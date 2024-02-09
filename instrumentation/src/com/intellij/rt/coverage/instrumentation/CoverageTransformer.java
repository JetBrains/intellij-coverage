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
import com.intellij.rt.coverage.instrumentation.data.ProjectContext;
import com.intellij.rt.coverage.instrumentation.dataAccess.*;
import com.intellij.rt.coverage.util.ClassNameUtil;
import com.intellij.rt.coverage.util.OptionsUtil;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;

import java.util.List;
import java.util.regex.Pattern;

public class CoverageTransformer extends AbstractIntellijClassfileTransformer {
  private final ProjectData myProjectData;
  protected final ProjectContext myProjectContext;
  private boolean myStop;

  public CoverageTransformer(ProjectData projectData, ProjectContext projectContext) {
    myProjectData = projectData;
    myProjectContext = projectContext;
  }

  @Override
  protected ClassVisitor createClassVisitor(String className, ClassLoader loader, ClassReader cr, ClassVisitor cw) {
    return InstrumentationStrategy.createInstrumenter(myProjectData, className, cr, cw, myProjectContext, createDataAccess(className, cr));
  }

  private CoverageDataAccess createDataAccess(String className, ClassReader cr) {
    if (OptionsUtil.FIELD_INSTRUMENTATION_ENABLED) {
      if (InstrumentationUtils.isCondyEnabled(cr)) {
        return new CondyCoverageDataAccess(createCondyInit(className, cr));
      } else {
        return new FieldCoverageDataAccess(cr, className, createInit(className, cr, false));
      }
    } else {
      return new NameCoverageDataAccess(createInit(className, cr, true));
    }
  }

  protected CoverageDataAccess.Init createInit(String className, ClassReader cr, boolean needCache) {
    boolean calculateHits = myProjectContext.getOptions().isCalculateHits;
    String arrayType = calculateHits ? DataAccessUtil.HITS_ARRAY_TYPE : DataAccessUtil.MASK_ARRAY_TYPE;
    String methodName = calculateHits ? (needCache ? "getHitsCached" : "getHits")
        : (needCache ? "getHitsMaskCached" : "getHitsMask");
    return new CoverageDataAccess.Init("__$hits$__", arrayType, CoverageRuntime.COVERAGE_RUNTIME_OWNER,
        methodName, "(Ljava/lang/String;)" + arrayType, new Object[]{className});
  }

  protected CoverageDataAccess.Init createCondyInit(String className, ClassReader cr) {
    boolean calculateHits = myProjectContext.getOptions().isCalculateHits;
    String arrayType = calculateHits ? DataAccessUtil.HITS_ARRAY_TYPE : DataAccessUtil.MASK_ARRAY_TYPE;
    String methodName = calculateHits ? "getHits" : "getHitsMask";
    return new CoverageDataAccess.Init("__$hits$__", arrayType, "com/intellij/rt/coverage/util/CondyUtils",
        methodName, "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;)" + arrayType, new Object[]{className});
  }

  @Override
  protected boolean shouldExclude(String className) {
    return ClassNameUtil.matchesPatterns(className, myProjectContext.getOptions().excludePatterns);
  }

  @Override
  protected InclusionPattern getInclusionPattern() {
    final List<Pattern> includes = myProjectContext.getOptions().includePatterns;
    return includes == null || includes.isEmpty() ? null : new InclusionPattern() {
      public boolean accept(String className) {
        return ClassNameUtil.matchesPatterns(className, includes);
      }
    };
  }

  @Override
  protected void visitClassLoader(ClassLoader classLoader) {
    ClassFinder classFinder = myProjectContext.getClassFinder();
    if (classFinder != null) {
      classFinder.addClassLoader(classLoader);
    }
  }

  @Override
  protected boolean isStopped() {
    return myStop;
  }

  public void stop() {
    myStop = true;
  }
}
