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

package com.intellij.rt.coverage.instrumentation.dataAccess;

import com.intellij.rt.coverage.instrumentation.InstrumentationUtils;
import org.jetbrains.coverage.org.objectweb.asm.ConstantDynamic;
import org.jetbrains.coverage.org.objectweb.asm.Handle;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

public class CondyCoverageDataAccess extends CoverageDataAccess {
  private final ConstantDynamic condy;

  public CondyCoverageDataAccess(String className, boolean isSampling) {
    final String getHitsMaskMethod = isSampling ? "getLineMask" : "getHitsMask";
    final Handle handle = new Handle(Opcodes.H_INVOKESTATIC, "com/intellij/rt/coverage/util/CondyUtils", getHitsMaskMethod,
        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;)[I", false);
    condy = new ConstantDynamic(HITS_NAME, InstrumentationUtils.OBJECT_TYPE, handle, className);
  }

  @Override
  public void onMethodStart(MethodVisitor mv, int localVariable) {
    mv.visitLdcInsn(condy);
    mv.visitTypeInsn(Opcodes.CHECKCAST, HITS_TYPE);
    mv.visitVarInsn(Opcodes.ASTORE, localVariable);
  }


  private static final String HITS_NAME = "__$hits$__";
  private static final String HITS_TYPE = "[I";
}
