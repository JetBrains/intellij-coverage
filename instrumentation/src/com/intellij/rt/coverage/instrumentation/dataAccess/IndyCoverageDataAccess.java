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
import org.jetbrains.coverage.org.objectweb.asm.*;

/**
 * Loads coverage data using an INVOKEDYNAMIC (indy).
 * After the first invocation, the array becomes bound to the call-site and no lookup is necessary.
 * Supported for class files version 7+.
 */
public class IndyCoverageDataAccess extends CoverageDataAccess {
  private final Handle myBsm;

  public IndyCoverageDataAccess(Init init) {
    super(init);
    myBsm = new Handle(Opcodes.H_INVOKESTATIC, init.initOwner, init.initName, init.initDesc, false);
  }

  @Override
  public void onMethodStart(MethodVisitor mv, int localVariable) {
    mv.visitInvokeDynamicInsn(
      myInit.name,
      "()" + InstrumentationUtils.OBJECT_TYPE,
      myBsm,
      myInit.params
    );
    if (!InstrumentationUtils.OBJECT_TYPE.equals(myInit.desc)) {
      mv.visitTypeInsn(Opcodes.CHECKCAST, myInit.desc);
    }
    mv.visitVarInsn(Opcodes.ASTORE, localVariable);
  }
}
