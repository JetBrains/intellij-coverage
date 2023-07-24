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

/**
 * Store coverage data in a constant dynamic (ConDy).
 * Supported for class files version 11+.
 */
public class CondyCoverageDataAccess extends CoverageDataAccess {
  private final ConstantDynamic myCondy;

  public CondyCoverageDataAccess(Init init) {
    super(init);
    final Handle handle = new Handle(Opcodes.H_INVOKESTATIC, init.initOwner, init.initName, init.initDesc, false);
    myCondy = new ConstantDynamic(init.name, InstrumentationUtils.OBJECT_TYPE, handle, init.params);
  }

  @Override
  public void onMethodStart(MethodVisitor mv, int localVariable) {
    mv.visitLdcInsn(myCondy);
    if (!InstrumentationUtils.OBJECT_TYPE.equals(myInit.desc)) {
      mv.visitTypeInsn(Opcodes.CHECKCAST, myInit.desc);
    }
    mv.visitVarInsn(Opcodes.ASTORE, localVariable);
  }
}
