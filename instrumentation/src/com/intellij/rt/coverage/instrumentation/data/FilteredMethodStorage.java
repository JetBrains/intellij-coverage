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

  private Set<String> myIncludedMethods;
  private Set<String> myIgnoredMethods;

  public boolean checkClassIncluded(InstrumentationData context) {
    if (isNoFilters(context)) return true;
    List<Pattern> includeAnnotations = context.getProjectContext().getOptions().includeAnnotations;
    List<Pattern> excludeAnnotations = context.getProjectContext().getOptions().excludeAnnotations;

    List<String> classAnnotations = InstrumentationUtils.getClassAnnotations(context.get(Key.CLASS_READER));

    // Class is not excluded if it is not annotated as included.
    // Such a filtration is applied on the method level.
    boolean included = !matchesPatterns(excludeAnnotations, classAnnotations)
        && !isOuterClassRegistered(context, false);
    if (!included) {
      addMethod(context.get(Key.CLASS_NAME), CLASS_MARKER, false);
    } else {
      boolean includeAnnotationsUsed = !includeAnnotations.isEmpty();
      boolean forceInclude = includeAnnotationsUsed &&
          (matchesPatterns(includeAnnotations, classAnnotations)
              || isOuterClassRegistered(context, true));
      if (forceInclude) {
        addMethod(context.get(Key.CLASS_NAME), CLASS_MARKER, true);
      }
    }
    return included;
  }

  public boolean checkMethodIncluded(InstrumentationData context) {
    if (isNoFilters(context)) return true;
    List<Pattern> includeAnnotations = context.getProjectContext().getOptions().includeAnnotations;
    List<Pattern> excludeAnnotations = context.getProjectContext().getOptions().excludeAnnotations;
    List<String> methodAnnotations = context.get(Key.METHOD_ANNOTATIONS);
    String owner = context.get(Key.CLASS_NAME);
    String filteredSignature = KotlinDefaultArgsBranchFilter.getOriginalNameAndDesc(context);
    boolean includeAnnotationsUsed = !includeAnnotations.isEmpty();

    boolean forceExclude = matchesPatterns(excludeAnnotations, methodAnnotations)
        || isMethodRegistered(owner, filteredSignature, false);

    boolean forceInclude = includeAnnotationsUsed &&
        (matchesPatterns(includeAnnotations, methodAnnotations)
            || isMethodRegistered(owner, filteredSignature, true));

    boolean included = !forceExclude && (!includeAnnotationsUsed || forceInclude);

    if (!included) {
      addMethod(owner, context.getMethodName() + context.getMethodDesc(), false);
    } else if (forceInclude) {
      addMethod(owner, context.getMethodName() + context.getMethodDesc(), true);
    }
    return included;
  }

  /**
   * This is a heuristic method to check if a method with the provided name is ignored.
   * The result could be incorrect in case of functions having the same name, but a different signature.
   * It is used for local function determination.
   */
  public synchronized boolean isMethodNameRegistered(String owner, String methodName, boolean included) {
    Set<String> methods = included ? myIncludedMethods : myIgnoredMethods;
    if (methods == null) return false;
    String target = owner + "#" + methodName;
    for (String candidate : methods) {
      if (candidate.startsWith(target)) return true;
    }
    return false;
  }

  public void addIgnoredMethod(String owner, String methodSignature) {
    addMethod(owner, methodSignature, false);
  }

  private boolean isNoFilters(InstrumentationData context) {
    return context.getProjectContext().getOptions().excludeAnnotations.isEmpty()
        && (myIgnoredMethods == null || myIgnoredMethods.isEmpty())
        && context.getProjectContext().getOptions().includeAnnotations.isEmpty()
        && (myIncludedMethods == null || myIncludedMethods.isEmpty());
  }

  private boolean isOuterClassRegistered(InstrumentationData context, boolean included) {
    return isRegisteredCompanionObject(context, included)
        || isAnonymousClassInRegisteredMethod(context, included);
  }

  private boolean isRegisteredCompanionObject(InstrumentationData context, boolean included) {
    String className = context.get(Key.CLASS_NAME);
    if (!className.endsWith(KotlinUtils.COMPANION_SUFFIX)) return false;
    String subjectName = className.substring(0, className.indexOf(KotlinUtils.COMPANION_SUFFIX));
    return isClassRegistered(subjectName, included);
  }

  private boolean isAnonymousClassInRegisteredMethod(InstrumentationData context, boolean included) {
    InstrumentationUtils.MethodDescriptor outerMethod = InstrumentationUtils.getOuterClass(context.get(Key.CLASS_READER));
    if (outerMethod == null) return false;
    String ownerName = ClassNameUtil.convertToFQName(outerMethod.owner);
    return isMethodRegistered(ownerName, outerMethod.name + outerMethod.descriptor, included);
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

  private boolean isMethodRegistered(String owner, String methodSignature, boolean included) {
    return isClassRegistered(owner, included) || isMethodRegisteredInternal(owner, methodSignature, included);
  }

  private boolean isClassRegistered(String owner, boolean included) {
    return isMethodRegisteredInternal(owner, CLASS_MARKER, included);
  }

  private synchronized boolean isMethodRegisteredInternal(String owner, String methodSignature, boolean included) {
    Set<String> methods = included ? myIncludedMethods : myIgnoredMethods;
    return methods != null && methods.contains(createDesc(owner, methodSignature));
  }

  public synchronized void addMethod(String owner, String methodSignature, boolean included) {
    Set<String> methods = included ? myIncludedMethods : myIgnoredMethods;
    if (methods == null) {
      methods = new HashSet<String>();
      if (included) {
        myIncludedMethods = methods;
      } else {
        myIgnoredMethods = methods;
      }
    }
    methods.add(createDesc(owner, methodSignature));
  }

  private static String createDesc(String owner, String methodSignature) {
    return owner + "#" + methodSignature;
  }
}
