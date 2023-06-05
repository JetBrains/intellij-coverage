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

package com.intellij.rt.coverage.instrumentation.filters.classFilter;

import com.intellij.rt.coverage.instrumentation.Instrumenter;
import com.intellij.rt.coverage.instrumentation.filters.lines.AnnotationIgnoredMethodFilter;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;

/**
 * This filter add companion object to the list of ignored classes, if the containing class is ignored by annotation.
 */
public class KotlinCompanionInIgnoredClassFilter extends ClassFilter {
  private static final String COMPANION_SUFFIX = "$Companion";
  private static final String IGNORED_CLASS_MARKER = "__$$IGNORED_CLASS_MARKER$$__";
  private static final String IGNORED_CLASS_MARKER_DESC = "";
  private boolean myShouldIgnore;
  private boolean myVisitedMethod;

  @Override
  public boolean isApplicable(Instrumenter context) {
    return AnnotationIgnoredMethodFilter.hasIgnoreAnnotations(context);
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    final String className = myContext.getClassName();
    if (className.endsWith(COMPANION_SUFFIX)) {
      final String outerName = className.substring(0, className.length() - COMPANION_SUFFIX.length());
      if (myContext.getProjectData().isMethodIgnored(outerName, IGNORED_CLASS_MARKER, IGNORED_CLASS_MARKER_DESC)) {
        myShouldIgnore = true;
        myContext.setIgnoreSection(true);
      }
    }
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    // Should be processed after all annotations are visited
    if (!myVisitedMethod) {
      myVisitedMethod = true;
      final String className = myContext.getClassName();
      if (myContext.isClassIgnoredByAnnotation()) {
        myContext.getProjectData().addIgnoredMethod(className, IGNORED_CLASS_MARKER, IGNORED_CLASS_MARKER_DESC);
      }
    }
    return super.visitMethod(access, name, descriptor, signature, exceptions);
  }

  @Override
  public void visitEnd() {
    super.visitEnd();
    if (myShouldIgnore) {
      myContext.setIgnoreSection(false);
    }
  }
}
