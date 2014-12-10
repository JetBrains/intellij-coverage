/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;

public class TouchCounter extends MethodVisitor implements Opcodes {
  private final int myVariablesCount;

  private final LineEnumerator myEnumerator;

  private Label myStartLabel;
  private Label myEndLabel;

  private int myCurrentLine;
  private int myCurrentJumpIdx;
  private int myCurrentSwitchIdx;

  private int myLastJump = -1;
  private int myLastLineJump = -1;

  private static final byte SEEN_NOTHING = 0;
  private static final byte GETSTATIC_SEEN = 1;

  private byte myState;

  public TouchCounter(final LineEnumerator enumerator, int access, String desc) {
    super(Opcodes.ASM5, enumerator.getWV());
    myEnumerator = enumerator;
    int variablesCount = ((Opcodes.ACC_STATIC & access) != 0) ? 0 : 1;
    final Type[] args = Type.getArgumentTypes(desc);
    for (int i = 0; i < args.length; i++) {
      variablesCount += args[i].getSize();
    }
    myVariablesCount = variablesCount;
  }


  public void visitLineNumber(int line, Label start) {
    myCurrentLine = line;
    myCurrentJumpIdx = 0;
    myCurrentSwitchIdx = 0;

    mv.visitVarInsn(Opcodes.ALOAD, getCurrentClassDataNumber());
    mv.visitIntInsn(Opcodes.SIPUSH, line);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "trace", "(Ljava/lang/Object;I)V");
    super.visitLineNumber(line, start);
  }

  public void visitLabel(Label label) {
    if (myStartLabel == null) {
      myStartLabel = label;
    }
    myEndLabel = label;

    super.visitLabel(label);


    final boolean isJump = myEnumerator.isJump(label);
    if (myLastJump != -1) {
      Label l = new Label();

      mv.visitVarInsn(Opcodes.ILOAD, getLineVariableNumber());
      mv.visitIntInsn(Opcodes.SIPUSH, myLastLineJump);
      mv.visitJumpInsn(Opcodes.IF_ICMPNE, l);

      mv.visitVarInsn(Opcodes.ILOAD, getJumpVariableNumber());
      mv.visitIntInsn(Opcodes.SIPUSH, myLastJump);
      mv.visitJumpInsn(Opcodes.IF_ICMPNE, l);

      touchLastJump();

      if (isJump) {
        Label l1 = new Label();
        mv.visitJumpInsn(Opcodes.GOTO, l1);
        mv.visitLabel(l);
        mv.visitVarInsn(Opcodes.ILOAD, getJumpVariableNumber());
        mv.visitJumpInsn(Opcodes.IFLT, l1);
        touchBranch(true);
        mv.visitLabel(l1);
      }
      else {
        mv.visitLabel(l);
      }
    }
    else if (isJump) {
      mv.visitVarInsn(Opcodes.ILOAD, getJumpVariableNumber());
      Label newLabelX = new Label();
      mv.visitJumpInsn(Opcodes.IFLT, newLabelX);
      touchBranch(true);
      mv.visitLabel(newLabelX);
    }


    final Integer key = myEnumerator.getSwitchKey(label);
    if (key != null) {
      mv.visitVarInsn(Opcodes.ALOAD, getCurrentClassDataNumber());
      mv.visitVarInsn(Opcodes.ILOAD, getLineVariableNumber());
      mv.visitVarInsn(Opcodes.ILOAD, getSwitchVariableNumber());
      mv.visitIntInsn(Opcodes.SIPUSH, key.intValue());
      mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "touchSwitch", "(Ljava/lang/Object;III)V");
    }
  }

  private void touchBranch(final boolean trueHit) {
    mv.visitVarInsn(Opcodes.ALOAD, getCurrentClassDataNumber());
    mv.visitVarInsn(Opcodes.ILOAD, getLineVariableNumber());
    mv.visitVarInsn(Opcodes.ILOAD, getJumpVariableNumber());
    mv.visitInsn(trueHit ? Opcodes.ICONST_0 : Opcodes.ICONST_1);
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "touchJump", "(Ljava/lang/Object;IIZ)V");

    mv.visitIntInsn(Opcodes.SIPUSH, -1);
    mv.visitVarInsn(Opcodes.ISTORE, getJumpVariableNumber());
  }

  private void touchLastJump() {
    if (myLastJump != -1) {
      myLastJump = -1;
      touchBranch(false);
    }
    myState = SEEN_NOTHING;
  }


  public void visitJumpInsn(final int opcode, final Label label) {
    byte state = myState;
    touchLastJump();
    if (opcode != Opcodes.GOTO && opcode != Opcodes.JSR && !myEnumerator.getMethodName().equals("<clinit>") && myEnumerator.isJump(label) && !(state == GETSTATIC_SEEN && opcode == Opcodes.IFNE)) {
      myLastJump = myCurrentJumpIdx;
      myLastLineJump = myCurrentLine;
      mv.visitIntInsn(Opcodes.SIPUSH, myCurrentLine);
      mv.visitVarInsn(Opcodes.ISTORE, getLineVariableNumber());
      mv.visitIntInsn(Opcodes.SIPUSH, myCurrentJumpIdx++);
      mv.visitVarInsn(Opcodes.ISTORE, getJumpVariableNumber());
    }
    super.visitJumpInsn(opcode, label);
  }


  public void visitCode() {
    mv.visitInsn(Opcodes.ICONST_0);
    mv.visitVarInsn(Opcodes.ISTORE, getLineVariableNumber());

    mv.visitIntInsn(Opcodes.SIPUSH, -1);
    mv.visitVarInsn(Opcodes.ISTORE, getJumpVariableNumber());

    mv.visitInsn(Opcodes.ICONST_0);
    mv.visitVarInsn(Opcodes.ISTORE, getSwitchVariableNumber());

    mv.visitLdcInsn(myEnumerator.getClassName());
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "loadClassData", "(Ljava/lang/String;)Ljava/lang/Object;");
    mv.visitVarInsn(Opcodes.ASTORE, getCurrentClassDataNumber());

    super.visitCode();
  }

  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    touchLastJump();
    storeSwitchDescriptor();
    super.visitLookupSwitchInsn(dflt, keys, labels);
  }

  public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
    touchLastJump();
    storeSwitchDescriptor();
    super.visitTableSwitchInsn(min, max, dflt, labels);
  }

  private void storeSwitchDescriptor() {
    mv.visitIntInsn(Opcodes.SIPUSH, myCurrentLine);
    mv.visitVarInsn(Opcodes.ISTORE, getLineVariableNumber());

    mv.visitIntInsn(Opcodes.SIPUSH, myCurrentSwitchIdx++);
    mv.visitVarInsn(Opcodes.ISTORE, getSwitchVariableNumber());
  }


  public void visitFieldInsn(int opcode, String owner, String name, String desc) {
    touchLastJump();
    if (opcode == Opcodes.GETSTATIC && name.equals("$assertionsDisabled")) {
      myState = GETSTATIC_SEEN;
    }
    super.visitFieldInsn(opcode, owner, name, desc);
  }


  public void visitInsn(int opcode) {
    touchLastJump();
    super.visitInsn(opcode);
  }

  public void visitIntInsn(int opcode, int operand) {
    touchLastJump();
    super.visitIntInsn(opcode, operand);
  }

  public void visitLdcInsn(Object cst) {
    touchLastJump();
    super.visitLdcInsn(cst);
  }

  public void visitMultiANewArrayInsn(String desc, int dims) {
    touchLastJump();
    super.visitMultiANewArrayInsn(desc, dims);
  }


  public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
    touchLastJump();
    super.visitTryCatchBlock(start, end, handler, type);
  }

  public void visitTypeInsn(int opcode, String desc) {
    touchLastJump();
    super.visitTypeInsn(opcode, desc);
  }

  public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
    touchLastJump();
    super.visitMethodInsn(opcode, owner, name, desc, itf);
  }


  public void visitVarInsn(int opcode, int var) {
    touchLastJump();
    mv.visitVarInsn(opcode, adjustVariable(var));
  }

  public void visitIincInsn(int var, int increment) {
    touchLastJump();
    mv.visitIincInsn(adjustVariable(var), increment);
  }

  public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
    touchLastJump();
    mv.visitLocalVariable(name, desc, signature, start, end, adjustVariable(index));
  }

  private int adjustVariable(final int var) {
    return (var >= getLineVariableNumber()) ? var + 4 : var;
  }

  public int getLineVariableNumber() {
    return myVariablesCount;
  }

  private int getJumpVariableNumber() {
    return myVariablesCount + 1;
  }

  private int getSwitchVariableNumber() {
    return myVariablesCount + 2;
  }

  public int getCurrentClassDataNumber() {
    return myVariablesCount + 3;
  }

  public void visitMaxs(int maxStack, int maxLocals) {
    if (myStartLabel != null && myEndLabel != null) {
      mv.visitLocalVariable("__line__number__", "I", null, myStartLabel, myEndLabel, getLineVariableNumber());
      mv.visitLocalVariable("__jump__number__", "I", null, myStartLabel, myEndLabel, getJumpVariableNumber());
      mv.visitLocalVariable("__switch__number__", "I", null, myStartLabel, myEndLabel, getSwitchVariableNumber());
      mv.visitLocalVariable("__class__data__", "Ljava/lang/Object;", null, myStartLabel, myEndLabel, getCurrentClassDataNumber());
    }
    super.visitMaxs(maxStack, maxLocals);
  }
}
