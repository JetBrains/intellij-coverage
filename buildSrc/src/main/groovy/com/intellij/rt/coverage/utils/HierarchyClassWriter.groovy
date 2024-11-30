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

package com.intellij.rt.coverage.utils

import groovy.transform.CompileStatic
import org.objectweb.asm.ClassWriter

@CompileStatic
class HierarchyClassWriter extends ClassWriter {
  private final Map<String, String> superClasses
  private final Map<String, LinkedHashSet<String>> hierarchies = [:]

  HierarchyClassWriter(Map<String, String> superClasses) {
    super(COMPUTE_FRAMES)
    this.superClasses = superClasses
  }

  @Override
  protected String getCommonSuperClass(String type1, String type2) {
    def hierarchy1 = getHierarchy(type1)
    def hierarchy2 = getHierarchy(type2)
    return hierarchy1.find { it in hierarchy2 }
  }

  private LinkedHashSet<String> getHierarchy(String type) {
    LinkedHashSet<String> result = hierarchies[type]
    if (result != null) {
      return result
    }
    result = []
    hierarchies[type] = result
    while (type != null) {
      result << type
      type = getSuperClass(type)
    }
    result << "java/lang/Object"
    return result
  }

  private String getSuperClass(String type) {
    String known = superClasses[type]
    if (known != null) {
      return known
    }
    def clazz = Class.forName(type.replace('/', '.'), false, classLoader)
    return clazz.superclass?.name?.replace('.', '/')
  }
}
