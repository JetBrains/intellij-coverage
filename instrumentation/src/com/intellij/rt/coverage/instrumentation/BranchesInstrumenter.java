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
import com.intellij.rt.coverage.data.instructions.ClassInstructions;
import com.intellij.rt.coverage.instrumentation.data.BranchDataContainer;
import com.intellij.rt.coverage.instrumentation.data.Jump;
import com.intellij.rt.coverage.instrumentation.data.Switch;
import com.intellij.rt.coverage.instrumentation.dataAccess.CoverageDataAccess;
import com.intellij.rt.coverage.instrumentation.dataAccess.DataAccessUtil;
import com.intellij.rt.coverage.instrumentation.filters.FilterUtils;
import com.intellij.rt.coverage.instrumentation.filters.branches.BranchesFilter;
import com.intellij.rt.coverage.instrumentation.util.LinesUtil;
import com.intellij.rt.coverage.instrumentation.util.LocalVariableInserter;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;

/**
 * Insert coverage hits in branch coverage mode.
 */
public class BranchesInstrumenter extends Instrumenter {

  private final CoverageDataAccess myDataAccess;
  private final BranchDataContainer myBranchData = new BranchDataContainer(this);

  public BranchesInstrumenter(ProjectData projectData, ClassVisitor classVisitor, String className, boolean shouldSaveSource, CoverageDataAccess dataAccess) {
    super(projectData, classVisitor, className, shouldSaveSource);
    myDataAccess = dataAccess;
  }

  @Override
  protected MethodVisitor createMethodLineEnumerator(MethodVisitor mv, String name, String desc, int access, String signature,
                                                     String[] exceptions) {
    myBranchData.resetMethod();
    if (myProjectData.isInstructionsCoverageEnabled()) {
      mv = new InstructionsEnumerator(this, myBranchData, mv, access, name, desc, signature, exceptions);
    } else {
      mv = new BranchesEnumerator(this, myBranchData, mv, access, name, desc, signature, exceptions);
    }
    return chainFilters(name, desc, access, signature, exceptions, mv);
  }

  /**
   * Create instrumenter that will insert coverage hits increments into bytecode.
   */
  public MethodVisitor createInstrumentingVisitor(MethodVisitor mv, BranchesEnumerator enumerator,
                                                  int access, String name, String desc) {
    if (enumerator.hasNoLines()) {
      return myDataAccess.createMethodVisitor(mv, name, false);
    }
    final MethodVisitor visitor = new HitsVisitor(mv, access, desc) {
      public void visitCode() {
        myDataAccess.onMethodStart(mv, getLVIndex());
        super.visitCode();
      }
    };
    return myDataAccess.createMethodVisitor(visitor, name, true);
  }

  @Override
  public void visitEnd() {
    myDataAccess.onClassEnd(this);
    super.visitEnd();
    if (myProjectData.isInstructionsCoverageEnabled()) {
      final ClassInstructions classInstructions = new ClassInstructions(myClassData, myBranchData.getInstructions());
      myProjectData.getInstructions().put(myClassData.getName(), classInstructions);
    }
  }

  @Override
  protected void initLineData() {
    myClassData.createHitsMask(myBranchData.getSize());
    myClassData.setLines(LinesUtil.calcLineArray(myMaxLineNumber, myLines));
  }

  private MethodVisitor chainFilters(String name, String desc, int access, String signature, String[] exceptions,
                                     MethodVisitor root) {
    for (BranchesFilter filter : FilterUtils.createBranchFilters()) {
      if (filter.isApplicable(this, access, name, desc, signature, exceptions)) {
        filter.initFilter(root, this, myBranchData);
        root = filter;
      }
    }
    return root;
  }

  public class HitsVisitor extends LocalVariableInserter {

    public HitsVisitor(MethodVisitor methodVisitor, int access, String descriptor) {
      super(methodVisitor, access, descriptor, "__$localHits$__", DataAccessUtil.HITS_ARRAY_TYPE);
    }

    public void visitLineNumber(final int line, final Label start) {
      final LineData lineData = getLineData(line);
      if (lineData != null) {
        incrementHitById(lineData.getId());
      }
      super.visitLineNumber(line, start);
    }

    @Override
    public void visitLabel(Label label) {
      super.visitLabel(label);

      final Jump jump = myBranchData.getJump(label);
      if (jump != null) {
        incrementHitById(jump.getId());
      }

      final Switch aSwitch = myBranchData.getSwitch(label);
      if (aSwitch != null) {
        incrementHitById(aSwitch.getId());
      }
    }

    private void incrementHitById(int id) {
      if (id == -1) return;
      InstrumentationUtils.touchById(mv, getLVIndex(), id);
    }
  }
}
