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

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.Instrumenter;
import com.intellij.rt.coverage.util.ClassNameUtil;
import org.jetbrains.coverage.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Filter out lines in method marked with annotation to ignore.
 */
public class AnnotationIgnoredMethodFilter extends LinesFilter {
  private boolean myShouldIgnore;
  private String myName;
  private String myDesc;

  @Override
  public boolean isApplicable(Instrumenter context, int access, String name, String desc, String signature, String[] exceptions) {
    return hasIgnoreAnnotations(context.getProjectData());
  }

  public static boolean hasIgnoreAnnotations(ProjectData projectData) {
    final List<Pattern> annotations = projectData.getAnnotationsToIgnore();
    return annotations != null && !annotations.isEmpty();
  }

  @Override
  public void initFilter(MethodVisitor methodVisitor, Instrumenter context, String name, String desc) {
    super.initFilter(methodVisitor, context, name, desc);
    myName = name;
    myDesc = desc;
  }

  @Override
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    final String annotationName = ClassNameUtil.convertVMNameToFQN(descriptor);
    if (!myShouldIgnore && ClassNameUtil.matchesPatterns(annotationName, myContext.getProjectData().getAnnotationsToIgnore())) {
      myContext.setIgnoreSection(true);
      myShouldIgnore = true;
    }
    return super.visitAnnotation(descriptor, visible);
  }

  @Override
  public void visitCode() {
    super.visitCode();
    if (myShouldIgnore) {
      myContext.getProjectData().getIgnoredStorage().addIgnoredMethod(myContext.getClassName(), myName, myDesc);
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
