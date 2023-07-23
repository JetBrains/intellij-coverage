/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

import com.intellij.rt.coverage.instrumentation.util.LocalVariableInserter;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

public class CoverageDataAccessVisitor extends ClassVisitor {
  private final CoverageDataAccess myDataAccess;
  private LocalVariableInserter myLVAccess;

  public CoverageDataAccessVisitor(ClassVisitor cv, CoverageDataAccess dataAccess) {
    super(Opcodes.API_VERSION, cv);
    myDataAccess = dataAccess;
  }

  public CoverageDataAccess getDataAccess() {
    return myDataAccess;
  }

  protected boolean shouldInstrumentMethod() {
    return true;
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
    if (!shouldInstrumentMethod()) {
      myLVAccess = null;
      return myDataAccess.createMethodVisitor(mv, name, false);
    }
    myLVAccess = new LocalVariableInserter(mv, access, descriptor, "__$coverage_local$__", myDataAccess.getInit().desc) {
      public void visitCode() {
        myDataAccess.onMethodStart(mv, getLVIndex());
        super.visitCode();
      }
    };
    return myDataAccess.createMethodVisitor(myLVAccess, name, true);
  }

  @Override
  public void visitEnd() {
    myDataAccess.onClassEnd(this);
    super.visitEnd();
  }

  public void loadFromLocal() {
    myLVAccess.loadFromLocal();
  }
}
