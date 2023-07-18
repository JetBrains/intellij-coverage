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

package com.intellij.rt.coverage.instrumentation.filters.classes;

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.InstrumentationUtils;
import com.intellij.rt.coverage.instrumentation.filters.lines.AnnotationIgnoredMethodFilter;
import com.intellij.rt.coverage.util.ClassNameUtil;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Filter classes ignored by annotation and their companion objects.
 * Also, filter anonymous classes declared in ignored/deprecated methods.
 */
public class ClassIgnoredByAnnotationFilter implements ClassSignatureFilter {
  private static final String COMPANION_SUFFIX = "$Companion";

  @Override
  public boolean shouldFilter(ClassReader cr, ProjectData projectData) {
    boolean ignoredAnonymous = isAnonymousClassInIgnoredMethod(cr, projectData);
    if (!AnnotationIgnoredMethodFilter.hasIgnoreAnnotations(projectData) && !ignoredAnonymous) return false;
    String className = ClassNameUtil.convertToFQName(cr.getClassName());
    boolean ignored = ignoredAnonymous
        || isIgnoredCompanionObject(className, projectData)
        || isClassIgnoredByAnnotation(projectData, InstrumentationUtils.getClassAnnotations(cr));
    if (ignored) {
      projectData.getIgnoredStorage().addIgnoredClass(className);
    }
    return ignored;
  }

  private boolean isIgnoredCompanionObject(String className, ProjectData projectData) {
    if (!className.endsWith(COMPANION_SUFFIX)) return false;
    String subjectName = className.substring(0, className.indexOf(COMPANION_SUFFIX));
    return projectData.getIgnoredStorage().isClassIgnored(subjectName);
  }

  private boolean isAnonymousClassInIgnoredMethod(ClassReader cr, ProjectData projectData) {
    InstrumentationUtils.MethodDescriptor outerMethod = InstrumentationUtils.getOuterClass(cr);
    if (outerMethod == null) return false;
    String ownerName = ClassNameUtil.convertToFQName(outerMethod.owner);
    return projectData.getIgnoredStorage().isMethodIgnored(ownerName, outerMethod.name, outerMethod.descriptor);
  }

  public static boolean isClassIgnoredByAnnotation(ProjectData projectData, List<String> classAnnotations) {
    List<Pattern> ignoreAnnotations = projectData.getAnnotationsToIgnore();
    if (ignoreAnnotations == null || ignoreAnnotations.isEmpty()) return false;
    for (String classAnnotation : classAnnotations) {
      final String annotationName = ClassNameUtil.convertVMNameToFQN(classAnnotation);
      if (ClassNameUtil.matchesPatterns(annotationName, ignoreAnnotations)) {
        return true;
      }
    }
    return false;
  }
}
