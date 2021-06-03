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
import com.intellij.rt.coverage.instrumentation.filters.FilterUtils;
import com.intellij.rt.coverage.instrumentation.filters.classFilter.PrivateConstructorOfUtilClassFilter;
import com.intellij.rt.coverage.util.ClassNameUtil;
import com.intellij.rt.coverage.instrumentation.testTracking.TestTrackingMode;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.ClassWriter;

import java.util.List;
import java.util.regex.Pattern;

public class CoverageClassfileTransformer extends AbstractIntellijClassfileTransformer {
  private final ProjectData data;
  private final boolean shouldCalculateSource;
  private final List<Pattern> excludePatterns;
  private final List<Pattern> includePatterns;
  private final ClassFinder cf;
  private final TestTrackingMode testTrackingMode;

  public CoverageClassfileTransformer(ProjectData data, boolean shouldCalculateSource, List<Pattern> excludePatterns, List<Pattern> includePatterns, ClassFinder cf) {
    this(data, shouldCalculateSource, excludePatterns, includePatterns, cf, null);
  }

  public CoverageClassfileTransformer(ProjectData data, boolean shouldCalculateSource, List<Pattern> excludePatterns, List<Pattern> includePatterns, ClassFinder cf, TestTrackingMode testTrackingMode) {
    this.data = data;
    this.shouldCalculateSource = shouldCalculateSource;
    this.excludePatterns = excludePatterns;
    this.includePatterns = includePatterns;
    this.cf = cf;
    this.testTrackingMode = testTrackingMode;
  }

  @Override
  protected ClassVisitor createClassVisitor(String className, ClassLoader loader, ClassReader cr, ClassWriter cw) {
    final Instrumenter instrumenter;
    if (data.isSampling()) {
      if (System.getProperty("idea.new.sampling.coverage") != null) {
        //wrap cw with new TraceClassVisitor(cw, new PrintWriter(new StringWriter())) to get readable bytecode
        instrumenter = new NewSamplingInstrumenter(data, cw, cr, className, shouldCalculateSource);
      } else {
        instrumenter = new SamplingInstrumenter(data, cw, className, shouldCalculateSource);
      }
    } else {
      if (System.getProperty("idea.new.tracing.coverage") != null) {
        if (data.isTestTracking()) {
          instrumenter = testTrackingMode.createInstrumenter(data, cw, cr, className, shouldCalculateSource);
        } else {
          instrumenter = new NewTracingInstrumenter(data, cw, cr, className, shouldCalculateSource);
        }
      } else {
        instrumenter = new TracingInstrumenter(data, cw, className, shouldCalculateSource);
      }
    }
    ClassVisitor result = instrumenter;
    if (FilterUtils.ignorePrivateConstructorOfUtilClassEnabled()) {
      result = PrivateConstructorOfUtilClassFilter.createWithContext(result, instrumenter);
    }
    return result;
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
    cf.addClassLoader(classLoader);
  }

  @Override
  protected boolean isStopped() {
    return data.isStopped();
  }
}
