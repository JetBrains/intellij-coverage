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

package com.intellij.rt.coverage.data;

import java.util.HashSet;
import java.util.Set;

public class IgnoredStorage {
  /**
   * We should mark class as ignored to be able to filter companion classes later.
   */
  private static final String IGNORED_CLASS_MARKER = "__$$IGNORED_CLASS_MARKER$$__";
  private static final String IGNORED_CLASS_MARKER_DESC = "";

  private Set<String> myIgnoredMethods;

  public boolean isMethodIgnored(String owner, String methodName, String desc) {
    return isClassIgnored(owner) || isMethodIgnoredInternal(owner, methodName, desc);
  }

  public boolean isClassIgnored(String owner) {
    return isMethodIgnoredInternal(owner, IGNORED_CLASS_MARKER, IGNORED_CLASS_MARKER_DESC);
  }

  public void addIgnoredClass(String owner) {
    addIgnoredMethod(owner, IGNORED_CLASS_MARKER, IGNORED_CLASS_MARKER_DESC);
  }

  public synchronized void addIgnoredMethod(String owner, String methodName, String desc) {
    if (myIgnoredMethods == null) {
      myIgnoredMethods = new HashSet<String>();
    }
    final String methodDesc = createDesc(owner, methodName, desc);
    myIgnoredMethods.add(methodDesc);
  }

  /**
   * This is a heuristic method to check if a method with the provided name is ignored.
   * The result could be incorrect in case of functions having the same name, but a different signature.
   * It is used for local function determination.
   */
  public synchronized boolean isMethodIgnored(String owner, String methodName) {
    if (myIgnoredMethods == null) return false;
    String target = owner + "#" + methodName;
    for (String candidate : myIgnoredMethods) {
      if (candidate.startsWith(target)) return true;
    }
    return false;
  }

  private synchronized boolean isMethodIgnoredInternal(String owner, String methodName, String desc) {
    return myIgnoredMethods != null && myIgnoredMethods.contains(createDesc(owner, methodName, desc));
  }

  private static String createDesc(String owner, String methodName, String desc) {
    return owner + "#" + methodName + desc;
  }
}
