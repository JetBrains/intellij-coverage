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
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Filter for undesirable branches. These filters are enabled only in branch coverage mode.
 */
public abstract class BranchesFilter extends MethodVisitor {
  protected Instrumenter myContext;
  protected BranchDataContainer myBranchData;

  public BranchesFilter() {
    super(Opcodes.API_VERSION);
  }

  public abstract boolean isApplicable(Instrumenter context, int access, String name,
                                       String desc, String signature, String[] exceptions);

  public void initFilter(MethodVisitor mv, Instrumenter context, BranchDataContainer branchData) {
    this.mv = mv;
    myContext = context;
    myBranchData = branchData;
  }
}
