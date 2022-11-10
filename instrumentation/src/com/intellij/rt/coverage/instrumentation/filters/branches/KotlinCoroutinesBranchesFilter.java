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

package com.intellij.rt.coverage.instrumentation.filters.branches;

import com.intellij.rt.coverage.instrumentation.Instrumenter;
import com.intellij.rt.coverage.instrumentation.data.BranchDataContainer;
import com.intellij.rt.coverage.instrumentation.filters.KotlinCoroutinesFilter;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;

public class KotlinCoroutinesBranchesFilter extends BranchesFilter {

  @Override
  public void initFilter(MethodVisitor mv, Instrumenter context, BranchDataContainer branchData) {
    super.initFilter(new InternalFilter(mv, context), context, branchData);
  }

  public boolean isApplicable(Instrumenter context, int access, String name,
                              String desc, String signature, String[] exceptions) {
    return KotlinCoroutinesFilter.isApplicable(context, name, desc);
  }

  private class InternalFilter extends KotlinCoroutinesFilter {

    public InternalFilter(MethodVisitor methodVisitor, Instrumenter context) {
      super(methodVisitor, context);
    }

    protected void onIgnoredJump() {
      myBranchData.removeLastJump();
    }

    protected void onIgnoredSwitch(Label dflt, Label... labels) {
      myBranchData.removeLastSwitch(dflt, labels);
    }
  }
}
