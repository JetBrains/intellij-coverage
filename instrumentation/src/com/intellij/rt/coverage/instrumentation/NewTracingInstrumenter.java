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

import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.data.BranchDataContainer;
import com.intellij.rt.coverage.instrumentation.data.Jump;
import com.intellij.rt.coverage.instrumentation.data.Switch;
import com.intellij.rt.coverage.util.LinesUtil;
import org.jetbrains.coverage.org.objectweb.asm.*;

public class NewTracingInstrumenter extends AbstractTracingInstrumenter {
  private static final String BRANCH_HITS_FIELD_NAME = "__$branchHits$__";
  private static final String BRANCH_HITS_FIELD_TYPE = "[I";
  private static final String BRANCH_HITS_FIELD_INIT_NAME = "__$branchHitsInit$__";
  private static final String BRANCH_HITS_LOCAL_VARIABLE_NAME = "__$localBranchHits$__";
  private static final String CLASS_INIT = "<clinit>";

  private final ExtraFieldInstrumenter myExtraFieldInstrumenter;

  public NewTracingInstrumenter(ProjectData projectData, ClassVisitor classVisitor, ClassReader cr, String className, boolean shouldCalculateSource) {
    super(projectData, classVisitor, className, shouldCalculateSource);
    myExtraFieldInstrumenter = new ExtraFieldTracingInstrumenter(cr, className);
  }

  @Override
  public MethodVisitor createTouchCounter(final MethodVisitor mv,
                                          final BranchDataContainer branchData,
                                          final LineEnumerator enumerator,
                                          final int access,
                                          final String name,
                                          final String desc,
                                          final String className) {
    if (myExtraFieldInstrumenter.isInterface() && CLASS_INIT.equals(name)) {
      return myExtraFieldInstrumenter.createMethodVisitor(this, mv, mv, name);
    }
    if (!enumerator.hasExecutableLines()) return mv;
    final MethodVisitor visitor = new LocalVariableInserter(mv, access, desc, BRANCH_HITS_LOCAL_VARIABLE_NAME, BRANCH_HITS_FIELD_TYPE) {
      public void visitLineNumber(final int line, final Label start) {
        LineData lineData = getLineData(line);
        if (lineData != null) {
          incrementHitById(lineData.getId());
        }
        super.visitLineNumber(line, start);
      }

      @Override
      public void visitLabel(Label label) {
        super.visitLabel(label);

        Jump jump = myBranchData.getJump(label);
        if (jump != null) {
          incrementHitById(jump.getId());
        }

        Switch aSwitch = myBranchData.getSwitch(label);
        if (aSwitch != null) {
          incrementHitById(aSwitch.getId());
        }
      }

      private void incrementHitById(int id) {
        if (id == -1) return;
        mv.visitVarInsn(Opcodes.ALOAD, getOrCreateLocalVariableIndex());
        InstrumentationUtils.pushInt(mv, id);
        InstrumentationUtils.incrementIntArrayByIndex(mv);
      }

      public void visitCode() {
        mv.visitFieldInsn(Opcodes.GETSTATIC, myExtraFieldInstrumenter.getInternalClassName(), BRANCH_HITS_FIELD_NAME, BRANCH_HITS_FIELD_TYPE);
        mv.visitVarInsn(Opcodes.ASTORE, getOrCreateLocalVariableIndex());
        super.visitCode();
      }
    };
    return myExtraFieldInstrumenter.createMethodVisitor(this, mv, visitor, name);
  }

  @Override
  public void visitEnd() {
    myExtraFieldInstrumenter.generateMembers(this);
    super.visitEnd();
  }

  @Override
  protected void initLineData() {
    final LineData[] lines = LinesUtil.calcLineArray(myMaxLineNumber, myLines);
    myClassData.createHitsMask(myBranchData.getSize());
    myClassData.setLines(lines);
  }

  private class ExtraFieldTracingInstrumenter extends ExtraFieldInstrumenter {

    public ExtraFieldTracingInstrumenter(ClassReader cr, String className) {
      super(cr, null, className, BRANCH_HITS_FIELD_NAME, BRANCH_HITS_FIELD_TYPE, BRANCH_HITS_FIELD_INIT_NAME, true);
    }

    public void initField(MethodVisitor mv) {
      mv.visitLdcInsn(getClassName());

      //get hits array
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "getHitsMask", "(Ljava/lang/String;)[I", false);

      //save hits array
      mv.visitFieldInsn(Opcodes.PUTSTATIC, myExtraFieldInstrumenter.getInternalClassName(), BRANCH_HITS_FIELD_NAME, BRANCH_HITS_FIELD_TYPE);
    }
  }
}
