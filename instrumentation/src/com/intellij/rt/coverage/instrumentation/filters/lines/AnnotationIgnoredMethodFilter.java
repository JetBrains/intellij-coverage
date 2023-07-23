/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation.filters.lines;

import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.data.Key;
import com.intellij.rt.coverage.util.ClassNameUtil;
import org.jetbrains.coverage.org.objectweb.asm.AnnotationVisitor;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Filter out lines in method marked with annotation to ignore.
 */
public class AnnotationIgnoredMethodFilter extends CoverageFilter {
  private boolean myShouldIgnore;

  @Override
  public boolean isApplicable(InstrumentationData context) {
    List<Pattern> annotations = context.get(Key.PROJECT_DATA).getAnnotationsToIgnore();
    return annotations != null && !annotations.isEmpty();
  }

  @Override
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    final String annotationName = ClassNameUtil.convertVMNameToFQN(descriptor);
    if (!myShouldIgnore && ClassNameUtil.matchesPatterns(annotationName, myContext.get(Key.PROJECT_DATA).getAnnotationsToIgnore())) {
      myContext.setIgnoreSection(true);
      myShouldIgnore = true;
    }
    return super.visitAnnotation(descriptor, visible);
  }

  @Override
  public void visitCode() {
    super.visitCode();
    if (myShouldIgnore) {
      myContext.get(Key.PROJECT_DATA).getIgnoredStorage().addIgnoredMethod(myContext.get(Key.CLASS_NAME), myContext.getMethodName(), myContext.getMethodDesc());
    }
  }

  @Override
  public void visitEnd() {
    super.visitEnd();
    if (myShouldIgnore) {
      myContext.setIgnoreSection(false);
    }
  }
}
