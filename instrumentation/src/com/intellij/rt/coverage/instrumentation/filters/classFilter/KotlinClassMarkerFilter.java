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

package com.intellij.rt.coverage.instrumentation.filters.classFilter;

import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.data.Key;
import org.jetbrains.coverage.org.objectweb.asm.AnnotationVisitor;

public class KotlinClassMarkerFilter extends ClassFilter {
  private static final String KOTLIN_METADATA = "Lkotlin/Metadata;";

  @Override
  public boolean isApplicable(InstrumentationData context) {
    return true;
  }

  @Override
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    if (KOTLIN_METADATA.equals(descriptor)) {
      myContext.put(Key.IS_KOTLIN, Boolean.TRUE);
    }
    return super.visitAnnotation(descriptor, visible);
  }
}
