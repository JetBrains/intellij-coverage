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

package com.intellij.rt.coverage.instrumentation.filters.classSignature;

import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Function or property reference instance causes Kotlin to generate a synthetic class.
 * Line numbers in this class are uncovered when a reference is not called.
 */
public class KotlinFunctionOrPropertyReferenceFilter implements ClassSignatureFilter {
  public boolean shouldFilter(ClassReader cr) {
    final String superClass = cr.getSuperName();
    final int access = cr.getAccess();
    return (access & Opcodes.ACC_SYNTHETIC) != 0
        && (access & Opcodes.ACC_FINAL) != 0
        && cr.getClassName().contains("$")
        && (isFunctionReferenceClass(superClass) || isPropertyReferenceClass(superClass));
  }

  private static boolean isFunctionReferenceClass(String superClass) {
    return superClass != null
        && superClass.startsWith("kotlin/jvm/internal/FunctionReference");
  }

  private static boolean isPropertyReferenceClass(String superClass) {
    return superClass != null
        && (superClass.startsWith("kotlin/jvm/internal/PropertyReference")
        || superClass.startsWith("kotlin/jvm/internal/MutablePropertyReference"));
  }
}
