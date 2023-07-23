/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation.filters.lines;

import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Filter for undesirable lines. These filters are enabled in line and branch coverage mode.
 */
public abstract class CoverageFilter extends MethodVisitor {
  protected InstrumentationData myContext;

  public CoverageFilter() {
    super(Opcodes.API_VERSION);
  }

  public void initFilter(MethodVisitor methodVisitor, InstrumentationData context) {
    mv = methodVisitor;
    myContext = context;
  }

  public abstract boolean isApplicable(InstrumentationData context);
}
