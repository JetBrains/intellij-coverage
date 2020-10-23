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

package com.intellij.rt.coverage.instrumentation.filters;

import com.intellij.rt.coverage.instrumentation.Instrumenter;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;

/**
 * Filters out coverage from method if matches filter.
 */
public abstract class MethodFilter extends MethodVisitor {

  protected abstract void filter();

  protected enum State {
    SHOULD_COVER, SHOULD_NOT_COVER, UNKNOWN
  }

  protected final Instrumenter myContext;
  protected State myState = State.UNKNOWN;

  public MethodFilter(int api, MethodVisitor methodVisitor, Instrumenter context) {
    super(api, methodVisitor);
    myContext = context;
  }

  protected boolean completed() {
    return myState != State.UNKNOWN;
  }

  @Override
  public void visitEnd() {
    super.visitEnd();
    if (myState == State.SHOULD_NOT_COVER) {
      filter();
    }
  }

  public interface Builder {
    MethodFilter createFilter(int api, MethodVisitor methodVisitor, Instrumenter context);
    boolean isApplicable(Instrumenter context);
  }
}
