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

import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;

/**
 * Filter classes ignored by annotation and their companion objects.
 * Also, filter anonymous classes declared in ignored/deprecated methods.
 */
public class ClassIgnoredByAnnotationFilter implements ClassSignatureFilter {

  @Override
  public boolean shouldIgnore(InstrumentationData context) {
    return !context.getProjectContext().getFilteredStorage().checkClassIncluded(context);
  }
}
