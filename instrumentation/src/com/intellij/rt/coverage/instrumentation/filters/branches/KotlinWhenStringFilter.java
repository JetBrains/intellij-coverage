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

package com.intellij.rt.coverage.instrumentation.filters.branches;

import com.intellij.rt.coverage.instrumentation.Instrumenter;
import com.intellij.rt.coverage.instrumentation.filters.KotlinUtils;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * In <code>when</code> by String construction, Kotlin compiler generates extra branches.
 * Firstly, check for null string if applicable:
 * <ol>
 *   <il>IFNULL (default branch)</il>
 * </ol>
 * <p>
 * Secondly, switch by hashCode:
 * <ol>
 *   <li>INVOKEVIRTUAL java/lang/String.hashCode ()I</li>
 *   <li>LOOKUPSWITCH</li>
 * </ol>
 * <p>
 * And then in each branch of the switch there is a check for equality:
 *
 * <ol>
 *  <li>LDC</li>
 *  <li>INVOKEVIRTUAL java/lang/String.equals (Ljava/lang/Object;)Z</li>
 *  <li>IFNE</li>
 *  <li>[OPTIONAL for the last branch]GOTO (default label)</li>
 * </ol>
 */
public class KotlinWhenStringFilter extends BranchesFilter {
  private int myState = 0;
  private Label myJumpLabel;
  private Label myDefaultLabel;

  @Override
  public boolean isApplicable(Instrumenter context, int access, String name, String desc, String signature, String[] exceptions) {
    return KotlinUtils.isKotlinClass(context);
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    if (myState == 0 && opcode == Opcodes.IFNULL) {
      myJumpLabel = label;
      myState++;
    } else if (myState == 5 && opcode == Opcodes.IFEQ && label == myDefaultLabel) {
      myState = 0;
      myBranchData.removeLastJump();
    } else if (myState == 5 && opcode == Opcodes.IFNE) {
      myState++;
    } else if (myState == 6 && opcode == Opcodes.GOTO && label == myDefaultLabel) {
      myState = 3;
      myBranchData.removeLastJump();
    } else {
      myState = 0;
    }
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    if ((myState == 0 || myState == 1)
        && opcode == Opcodes.INVOKEVIRTUAL
        && "java/lang/String".equals(owner)
        && "hashCode".equals(name)
        && "()I".equals(descriptor)) {
      myState = 2;
      return;
    } else if (myState == 4 && opcode == Opcodes.INVOKEVIRTUAL
        && "java/lang/String".equals(owner)
        && "equals".equals(name)
        && "(Ljava/lang/Object;)Z".equals(descriptor)) {
      myState++;
      return;
    }
    myState = 0;
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    super.visitLookupSwitchInsn(dflt, keys, labels);
    visitSwitch(dflt, labels);
  }


  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    super.visitTableSwitchInsn(min, max, dflt, labels);
    visitSwitch(dflt, labels);
  }

  private void visitSwitch(Label dflt, Label[] labels) {
    if (myState == 2) {
      if (myJumpLabel == dflt) {
        myJumpLabel = null;
        myBranchData.removeLastJump();
      }
      myDefaultLabel = dflt;
      myState++;
      return;
    }
    myState = 0;
  }

  @Override
  public void visitLdcInsn(Object value) {
    super.visitLdcInsn(value);
    if ((myState == 3 || myState == 6) && value instanceof String) {
      myState = 4;
      return;
    }
    myState = 0;
  }
}
