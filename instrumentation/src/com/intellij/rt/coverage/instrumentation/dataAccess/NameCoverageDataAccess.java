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

import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Access coverage data from the coverage engine in the beginning of each method
 * by name of the class.
 */
public class NameCoverageDataAccess extends CoverageDataAccess {

  public NameCoverageDataAccess(Init init) {
    super(init);
  }

  @Override
  public void onMethodStart(MethodVisitor mv, int localVariable) {
    myInit.loadParams(mv);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, myInit.initOwner, myInit.initName, myInit.initDesc, false);
    mv.visitVarInsn(Opcodes.ASTORE, localVariable);
  }
}
