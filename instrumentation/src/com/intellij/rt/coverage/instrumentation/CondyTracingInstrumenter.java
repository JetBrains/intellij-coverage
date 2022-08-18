/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.data.BranchDataContainer;
import org.jetbrains.coverage.org.objectweb.asm.*;

public class CondyTracingInstrumenter extends AbstractTracingInstrumenter {
  private static final String BRANCH_HITS_FIELD_NAME = "__$branchHits$__";
  private static final String BRANCH_HITS_FIELD_TYPE = "[I";

  private final Handle handle = new Handle(Opcodes.H_INVOKESTATIC, "com/intellij/rt/coverage/util/CondyUtils", "getHitsMask", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;)[I", false);
  private final ConstantDynamic condy = new ConstantDynamic(BRANCH_HITS_FIELD_NAME, InstrumentationUtils.OBJECT_TYPE, handle, getClassName());


  public CondyTracingInstrumenter(ProjectData projectData, ClassVisitor classVisitor, String className, boolean shouldCalculateSource) {
    super(projectData, classVisitor, className, shouldCalculateSource);
  }

  @Override
  public MethodVisitor createTouchCounter(final MethodVisitor mv,
                                          final BranchDataContainer branchData,
                                          final LineEnumerator enumerator,
                                          final int access,
                                          final String name,
                                          final String desc,
                                          final String className) {
    if (enumerator.hasNoLines()) return mv;
    return new NewTracingInstrumenter.ArrayTracingMethodVisitor(mv, access, desc, enumerator) {
      public void visitCode() {
        mv.visitLdcInsn(condy);
        mv.visitTypeInsn(Opcodes.CHECKCAST, BRANCH_HITS_FIELD_TYPE);
        mv.visitVarInsn(Opcodes.ASTORE, getOrCreateLocalVariableIndex());
        super.visitCode();
      }
    };
  }

  @Override
  protected void initLineData() {
    myClassData.createHitsMask(myBranchData.getSize());
    super.initLineData();
  }
}
