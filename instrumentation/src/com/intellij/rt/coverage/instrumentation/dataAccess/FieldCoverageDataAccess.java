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

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.ExtraFieldInstrumenter;
import com.intellij.rt.coverage.instrumentation.InstrumentationUtils;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

public class FieldCoverageDataAccess extends CoverageDataAccess {
  private final ExtraFieldInstrumenter myExtraFieldInstrumenter;

  public FieldCoverageDataAccess(ClassReader cr, final String className, final boolean isSampling) {
    myExtraFieldInstrumenter = new ExtraFieldInstrumenter(cr, null, className, HITS_NAME, HITS_TYPE, true) {

      public void initField(MethodVisitor mv) {
        mv.visitLdcInsn(className);

        //get hits array
        final String getHitsMaskMethod = isSampling ? "getLineMask" : "getHitsMask";
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, getHitsMaskMethod, "(Ljava/lang/String;)[I", false);

        //save hits array
        mv.visitFieldInsn(Opcodes.PUTSTATIC, getInternalClassName(), HITS_NAME, HITS_TYPE);
      }
    };
  }

  @Override
  public void onMethodStart(MethodVisitor mv, int localVariable) {
    mv.visitFieldInsn(Opcodes.GETSTATIC, myExtraFieldInstrumenter.getInternalClassName(), HITS_NAME, HITS_TYPE);
    mv.visitVarInsn(Opcodes.ASTORE, localVariable);
  }

  @Override
  public void onClassEnd(ClassVisitor cv) {
    myExtraFieldInstrumenter.generateMembers(cv);
  }

  @Override
  public MethodVisitor createMethodVisitor(MethodVisitor mv, String name, boolean hasLines) {
    if (hasLines || myExtraFieldInstrumenter.isInterface() && InstrumentationUtils.CLASS_INIT.equals(name)) {
      return myExtraFieldInstrumenter.createMethodVisitor(mv, name);
    }
    return mv;
  }

  private static final String HITS_NAME = "__$hits$__";
  private static final String HITS_TYPE = "[I";
}
