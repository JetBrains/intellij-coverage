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

package com.intellij.rt.coverage.instrumentation.filters.lines;

import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.data.Key;
import com.intellij.rt.coverage.instrumentation.filters.branches.KotlinDefaultArgsBranchFilter;
import org.jetbrains.coverage.org.objectweb.asm.Handle;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Filter for Kotlin default args function generated first line.
 * <p>
 * Kotlin 1.6 includes extra line, and all the generated code is located at that line.
 */
public class KotlinDefaultArgsLineFilter extends CoverageFilter {
  private int myFirstLine = -1;
  private int myCurrentLine = -1;
  private int myState = 0;
  private boolean myHasInstructions = false;
  private String myName;
  private String myNameDesc;
  private String myInternalName;

  /**
   * Index of first mask variable.
   */
  private int myMaxMaskIndex = -1;

  /**
   * Index of last mask variable.
   */
  private int myMinMaskIndex = -1;


  public boolean isApplicable(InstrumentationData context) {
    return KotlinDefaultArgsBranchFilter.isFilterApplicable(context);
  }

  @Override
  public void initFilter(MethodVisitor methodVisitor, InstrumentationData context) {
    super.initFilter(methodVisitor, context);
    String name = context.getMethodName();
    String desc = context.getMethodDesc();
    myNameDesc = KotlinDefaultArgsBranchFilter.getOriginalNameAndDesc(name, desc);
    myName = myNameDesc.substring(0, myNameDesc.indexOf('('));
    myInternalName = KotlinImplementerDefaultInterfaceMemberFilter
        .removeDefaultMarkerSuffix(myContext.get(Key.CLASS_INTERNAL_NAME));
    final int[] range = KotlinDefaultArgsBranchFilter.getMaskIndexRange(name, desc);
    myMinMaskIndex = range[0];
    myMaxMaskIndex = range[1];
    myHasInstructions = false;
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    if (myFirstLine == -1) {
      myFirstLine = line;
    }
    myCurrentLine = line;
    super.visitLineNumber(line, start);

    // replace method signature, so that generated methods are invisible
    final LineData lineData = myContext.getLineData(line);
    if (lineData != null) {
      lineData.setMethodSignature(myNameDesc);
    }
  }

  @Override
  public void visitEnd() {
    if (myFirstLine != -1 && !myHasInstructions) {
      myContext.removeLine(myFirstLine);
    }
    super.visitEnd();
  }

  @Override
  public void visitVarInsn(int opcode, int var) {
    super.visitVarInsn(opcode, var);
    if (myCurrentLine != myFirstLine) return;
    if ((myState == 0 || myState == 4) && myMinMaskIndex <= var && var <= myMaxMaskIndex) {
      myState = 1;
      return;
    }
    if ((myState == 4 || myState == 10) && var < myMinMaskIndex) {
      myState = 10; // default method invocation started
      return;
    }
    markHasInstructions();
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    if (myCurrentLine != myFirstLine) return;
    if (myState == 2 && opcode == Opcodes.IAND) {
      myState = 3;
      return;
    }
    if (myState == 1 && (opcode == Opcodes.ICONST_1 || opcode == Opcodes.ICONST_2 || opcode == Opcodes.ICONST_4)) {
      myState = 2;
      return;
    }
    if (myState == 11 && Opcodes.IRETURN <= opcode && opcode <= Opcodes.RETURN) {
      return;
    }
    markHasInstructions();
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    super.visitIntInsn(opcode, operand);
    if (myCurrentLine != myFirstLine) return;
    if (myState == 1 && (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH)) {
      myState = 2;
      return;
    }
    markHasInstructions();
  }

  @Override
  public void visitLdcInsn(Object value) {
    super.visitLdcInsn(value);
    if (myCurrentLine != myFirstLine) return;
    if (myState == 1 && value instanceof Integer) {
      myState = 2;
      return;
    }
    markHasInstructions();
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    if (myCurrentLine != myFirstLine) return;
    if (myState == 3 && opcode == Opcodes.IFEQ) {
      myState = 4;
      return;
    }
    markHasInstructions();
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    if (myCurrentLine != myFirstLine) return;
    if (myState == 10 && myName.equals(name) && myInternalName.equals(owner)) {
      myState = 11;
      return;
    }
    markHasInstructions();
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    super.visitFieldInsn(opcode, owner, name, descriptor);
    markHasInstructions();
  }

  @Override
  public void visitIincInsn(int var, int increment) {
    super.visitIincInsn(var, increment);
    markHasInstructions();
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    super.visitLookupSwitchInsn(dflt, keys, labels);
    markHasInstructions();
  }

  @Override
  public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
    super.visitMultiANewArrayInsn(descriptor, numDimensions);
    markHasInstructions();
  }

  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    super.visitTableSwitchInsn(min, max, dflt, labels);
    markHasInstructions();
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    super.visitTypeInsn(opcode, type);
    markHasInstructions();
  }

  @Override
  public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    markHasInstructions();
  }

  private void markHasInstructions() {
    // We care only about the first line here, as only it may be generated
    if (myCurrentLine != myFirstLine) return;
    // Do not care about the code before the users code, as it is all generated
    if (myState < 4) return;
    myHasInstructions = true;
  }
}
