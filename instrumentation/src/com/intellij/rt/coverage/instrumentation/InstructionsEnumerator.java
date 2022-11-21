/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

import com.intellij.rt.coverage.data.JumpData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.SwitchData;
import com.intellij.rt.coverage.instrumentation.data.BranchDataContainer;
import com.intellij.rt.coverage.instrumentation.data.Jump;
import com.intellij.rt.coverage.instrumentation.data.Switch;
import org.jetbrains.coverage.org.objectweb.asm.Handle;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;

/**
 * Collects information about number of instructions located at line/jump/switch.
 * Note that this enumerator only collects data and does not affect runtime execution,
 * so it may be enabled at unloaded classes analysis stage.
 */
public class InstructionsEnumerator extends BranchesEnumerator {
  private final Map<Label, Jump> myOriginalLabelToJump = new HashMap<Label, Jump>();
  private final Map<Label, Switch> myOriginalLabelToSwitch = new HashMap<Label, Switch>();
  private Jump myLastJump;
  private Label myLastLabel;
  private int myInstructionCounter;
  private boolean myHasInstructions = false;


  public InstructionsEnumerator(BranchesInstrumenter instrumenter, BranchDataContainer branchData, MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions) {
    super(instrumenter, branchData, mv, access, name, desc, signature, exceptions);
  }

  private void saveInstructionsToOwner() {
    final LineData lineData = getInstrumenter().getLineData(myCurrentLine);
    if (myInstructionCounter > 0 && lineData != null) {
      final Jump jump = myLastJump != null ? myLastJump : (myLastLabel == null ? null : myOriginalLabelToJump.get(myLastLabel));
      final Switch aSwitch = myLastLabel == null ? null : myOriginalLabelToSwitch.get(myLastLabel);

      boolean applied = false;

      if (jump != null) {
        final int jumpId = jump.getId();
        for (int index = 0; index < lineData.jumpsCount(); index++) {
          final JumpData jumpData = lineData.getJumpData(index);
          if (jumpData.getId(true) == jumpId) {
            myBranchData.addInstructions(jumpId, myInstructionCounter);
            applied = true;
            break;
          }

          if (jumpData.getId(false) == jumpId) {
            myBranchData.addInstructions(jumpId, myInstructionCounter);
            applied = true;
            break;
          }
        }
      } else if (aSwitch != null) {
        final int switchId = aSwitch.getId();
        loop:
        for (int index = 0; index < lineData.switchesCount(); index++) {
          final SwitchData switchData = lineData.getSwitchData(index);
          for (int i = -1; i < switchData.getKeys().length; i++) {
            if (switchData.getId(i) == switchId) {
              myBranchData.addInstructions(switchId, myInstructionCounter);
              applied = true;
              break loop;
            }
          }
        }
      }
      if (!applied) {
        myBranchData.addInstructions(lineData.getId(), myInstructionCounter);
      }
    }
    myLastLabel = null;
    myLastJump = null;
    myInstructionCounter = 0;
  }

  @Override
  protected void onNewJump(Label originalLabel, Label trueLabel, Label falseLabel) {
    myLastJump = myBranchData.getJump(falseLabel);
    myOriginalLabelToJump.put(originalLabel, myBranchData.getJump(trueLabel));
  }

  @Override
  protected void onNewSwitch(SwitchLabels original, SwitchLabels replacement) {
    myOriginalLabelToSwitch.put(original.getDefault(), myBranchData.getSwitch(replacement.getDefault()));
    for (int i = 0; i < original.getLabels().length; i++) {
      myOriginalLabelToSwitch.put(original.getLabels()[i], myBranchData.getSwitch(replacement.getLabels()[i]));
    }
  }

  @Override
  public void visitEnd() {
    saveInstructionsToOwner();
    super.visitEnd();
  }

  @Override
  public void visitLabel(Label label) {
    super.visitLabel(label);
    saveInstructionsToOwner();
    if (myOriginalLabelToJump.containsKey(label) || myOriginalLabelToSwitch.containsKey(label)) {
      myLastLabel = label;
    } else {
      myLastLabel = null;
    }
  }

  @Override
  public void visitJumpInsn(final int opcode, final Label label) {
    touch();
    saveInstructionsToOwner();
    super.visitJumpInsn(opcode, label);
  }

  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    touch();
    saveInstructionsToOwner();
    super.visitLookupSwitchInsn(dflt, keys, labels);
  }

  public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
    touch();
    saveInstructionsToOwner();
    super.visitTableSwitchInsn(min, max, dflt, labels);
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    super.visitTypeInsn(opcode, type);
    touch();
  }

  @Override
  public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
    super.visitMultiANewArrayInsn(descriptor, numDimensions);
    touch();
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    touch();
  }

  @Override
  public void visitLdcInsn(Object value) {
    super.visitLdcInsn(value);
    touch();
  }

  @Override
  public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    touch();
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    super.visitIntInsn(opcode, operand);
    touch();
  }

  @Override
  public void visitIincInsn(int var, int increment) {
    super.visitIincInsn(var, increment);
    touch();
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    super.visitFieldInsn(opcode, owner, name, descriptor);
    touch();
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    if (myHasInstructions && Opcodes.IRETURN <= opcode && opcode <= Opcodes.RETURN) {
      return;
    }
    touch();
  }

  @Override
  public void visitVarInsn(int opcode, int var) {
    super.visitVarInsn(opcode, var);
    touch();
  }

  private void touch() {
    myInstructionCounter++;
    myHasInstructions = true;
  }
}
