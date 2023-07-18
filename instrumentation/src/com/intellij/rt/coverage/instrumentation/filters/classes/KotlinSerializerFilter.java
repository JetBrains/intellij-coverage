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

import com.intellij.rt.coverage.instrumentation.InstrumentationUtils;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;

/**
 * Filter synthetic kotlinx serializer classes.
 */
public class KotlinSerializerFilter implements ClassSignatureFilter {

  public static final String SERIALIZER_SUFFIX = "$$serializer";

  @Override
  public boolean shouldFilter(ClassReader cr) {
    String className = cr.getClassName();
    if (!className.endsWith(SERIALIZER_SUFFIX)) return false;
    String[] interfaces = cr.getInterfaces();
    if (interfaces.length != 1 ||
        !"kotlinx/serialization/internal/GeneratedSerializer".equals(interfaces[0])) return false;
    String parentName = InstrumentationUtils.getParentClassIfIsInner(cr);
    return parentName != null && (parentName + SERIALIZER_SUFFIX).equals(className);
  }
}
