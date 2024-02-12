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

package com.intellij.rt.coverage.instrumentation.data;

import com.intellij.rt.coverage.instrumentation.InstrumentationUtils;
import com.intellij.rt.coverage.instrumentation.filters.KotlinUtils;
import com.intellij.rt.coverage.instrumentation.filters.branches.KotlinDefaultArgsBranchFilter;
import com.intellij.rt.coverage.util.ClassNameUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class FilteredMethodStorage {
  /**
   * We should mark class as ignored to be able to filter companion classes later.
   */
  private static final String CLASS_MARKER = "__$$CLASS_MARKER$$__()V";

  private Set<String> myIgnoredMethods;

  public boolean checkClassIncluded(InstrumentationData context) {
    List<Pattern> excludeAnnotations = context.getProjectContext().getOptions().excludeAnnotations;
    if (excludeAnnotations.isEmpty() && (myIgnoredMethods == null || myIgnoredMethods.isEmpty())) return true;
    List<String> classAnnotations = InstrumentationUtils.getClassAnnotations(context.get(Key.CLASS_READER));
    boolean included = !matchesPatterns(excludeAnnotations, classAnnotations)
        && !(isIgnoredCompanionObject(context) || isAnonymousClassInIgnoredMethod(context));
    if (!included) {
      addIgnoredClass(context.get(Key.CLASS_NAME));
    }
    return included;
  }

  public boolean checkMethodIncluded(InstrumentationData context) {
    List<Pattern> excludeAnnotations = context.getProjectContext().getOptions().excludeAnnotations;
    if (excludeAnnotations.isEmpty() && (myIgnoredMethods == null || myIgnoredMethods.isEmpty())) return true;
    List<String> methodAnnotations = context.get(Key.METHOD_ANNOTATIONS);
    String owner = context.get(Key.CLASS_NAME);
    String filteredSignature = KotlinDefaultArgsBranchFilter.getOriginalNameAndDesc(context);
    boolean isIncluded = !(methodAnnotations != null && matchesPatterns(excludeAnnotations, methodAnnotations))
        && !isMethodIgnored(owner, filteredSignature);

    if (!isIncluded) {
      String originalSignature = context.getMethodName() + context.getMethodDesc();
      addIgnoredMethod(owner, originalSignature);
    }
    return isIncluded;
  }


  private boolean isIgnoredCompanionObject(InstrumentationData context) {
    String className = context.get(Key.CLASS_NAME);
    if (!className.endsWith(KotlinUtils.COMPANION_SUFFIX)) return false;
    String subjectName = className.substring(0, className.indexOf(KotlinUtils.COMPANION_SUFFIX));
    return isClassIgnored(subjectName);
  }

  private boolean isAnonymousClassInIgnoredMethod(InstrumentationData context) {
    InstrumentationUtils.MethodDescriptor outerMethod = InstrumentationUtils.getOuterClass(context.get(Key.CLASS_READER));
    if (outerMethod == null) return false;
    String ownerName = ClassNameUtil.convertToFQName(outerMethod.owner);
    return isMethodIgnored(ownerName, outerMethod.name + outerMethod.descriptor);
  }

  private static boolean matchesPatterns(List<Pattern> patterns, List<String> annotations) {
    if (patterns == null || patterns.isEmpty()) return false;
    if (annotations == null || annotations.isEmpty()) return false;
    for (String annotation : annotations) {
      String annotationName = ClassNameUtil.convertVMNameToFQN(annotation);
      if (ClassNameUtil.matchesPatterns(annotationName, patterns)) {
        return true;
      }
    }
    return false;
  }

  private boolean isMethodIgnored(String owner, String methodSignature) {
    return isClassIgnored(owner) || isMethodIgnoredInternal(owner, methodSignature);
  }

  private boolean isClassIgnored(String owner) {
    return isMethodIgnoredInternal(owner, CLASS_MARKER);
  }

  private void addIgnoredClass(String owner) {
    addIgnoredMethod(owner, CLASS_MARKER);
  }

  public synchronized void addIgnoredMethod(String owner, String methodSignature) {
    if (myIgnoredMethods == null) {
      myIgnoredMethods = new HashSet<String>();
    }
    final String methodDesc = createDesc(owner, methodSignature);
    myIgnoredMethods.add(methodDesc);
  }

  /**
   * This is a heuristic method to check if a method with the provided name is ignored.
   * The result could be incorrect in case of functions having the same name, but a different signature.
   * It is used for local function determination.
   */
  public synchronized boolean isMethodNameIgnored(String owner, String methodName) {
    if (myIgnoredMethods == null) return false;
    String target = owner + "#" + methodName;
    for (String candidate : myIgnoredMethods) {
      if (candidate.startsWith(target)) return true;
    }
    return false;
  }

  private synchronized boolean isMethodIgnoredInternal(String owner, String methodSignature) {
    return myIgnoredMethods != null && myIgnoredMethods.contains(createDesc(owner, methodSignature));
  }

  private static String createDesc(String owner, String methodSignature) {
    return owner + "#" + methodSignature;
  }
}
