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

package com.intellij.rt.coverage.instrumentation.filters.visiting;

import com.intellij.rt.coverage.instrumentation.Instrumenter;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Filters out coverage from method if matches filter.
 */
public abstract class MethodVisitingFilter extends MethodVisitor {
  protected Instrumenter myContext;

  public MethodVisitingFilter() {
    super(Opcodes.API_VERSION);
  }

  public void initFilter(MethodVisitor methodVisitor, Instrumenter context) {
    mv = methodVisitor;
    myContext = context;
  }

  public abstract boolean isApplicable(Instrumenter context);
}
