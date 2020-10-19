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

package com.intellij.rt.coverage.instrumentation.filters.enumerating;

import com.intellij.rt.coverage.instrumentation.Instrumenter;
import com.intellij.rt.coverage.instrumentation.kotlin.KotlinUtils;
import org.jetbrains.coverage.org.objectweb.asm.Handle;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Filter out generated Kotlin coroutines state machines.
 * Namely, TABLESWITCH on state and 'if suspend' checks are ignored.
 */
public class KotlinCoroutinesFilter extends LineEnumeratorFilter {
  private boolean myGetCoroutinesSuspendedVisited = false;
  private boolean myStoreCoroutinesSuspendedVisited = false;
  private boolean myLoadCoroutinesSuspendedVisited = false;
  private boolean myStateLabelVisited = false;
  private boolean mySuspendCallVisited = false;
  private int myCoroutinesSuspendedIndex = -1;
  private String myClassName = null;
  private int myLine = -1;
  private boolean myHadLineDataBefore = false;
  private boolean myHasExecutableCode = false;

  /**
   * This filter is applicable for suspend methods of Kotlin class.
   * It could be a suspend lambda, then it's name is 'invokeSuspend'.
   * Or it could be a suspend method, then it's last parameter is a Continuation.
   */
  @Override
  public boolean isApplicable(Instrumenter context, int access, String name, String desc, String signature, String[] exceptions) {
    return KotlinUtils.isKotlinClass(context)
        && (name.equals("invokeSuspend") || desc.endsWith("Lkotlin/coroutines/Continuation;)Ljava/lang/Object;"));
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    super.visitLineNumber(line, start);
    myHadLineDataBefore = myContext.getInstrumenter().getLineData(line) != null;
    myLine = line;
    myHasExecutableCode = false;
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    myHasExecutableCode = true;

    boolean getCoroutinesSuspendedVisited = opcode == Opcodes.INVOKESTATIC
        && owner.equals("kotlin/coroutines/intrinsics/IntrinsicsKt")
        && name.equals("getCOROUTINE_SUSPENDED")
        && descriptor.equals("()Ljava/lang/Object;");
    boolean suspendCallVisited = descriptor.endsWith("Lkotlin/coroutines/Continuation;)Ljava/lang/Object;");
    if (getCoroutinesSuspendedVisited || suspendCallVisited) {
      myGetCoroutinesSuspendedVisited |= getCoroutinesSuspendedVisited;
      mySuspendCallVisited |= suspendCallVisited;
    } else {
      myGetCoroutinesSuspendedVisited = false;
      mySuspendCallVisited = false;
    }
  }

  @Override
  public void visitVarInsn(int opcode, int var) {
    super.visitVarInsn(opcode, var);
    myHasExecutableCode = true;
    if (!myStoreCoroutinesSuspendedVisited && myGetCoroutinesSuspendedVisited && opcode == Opcodes.ASTORE) {
      myStoreCoroutinesSuspendedVisited = true;
      myCoroutinesSuspendedIndex = var;
    }
    myLoadCoroutinesSuspendedVisited = myStoreCoroutinesSuspendedVisited
        && opcode == Opcodes.ALOAD
        && var == myCoroutinesSuspendedIndex;
  }

  /**
   * Ignore 'if' if it compares with 'COROUTINE_SUSPENDED' object.
   * Comparison could be with stored variable or with just returned from 'getCOROUTINE_SUSPENDED' value.
   */
  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    myHasExecutableCode = true;
    boolean compareWithCoroutinesSuspend = myLoadCoroutinesSuspendedVisited || myGetCoroutinesSuspendedVisited;
    if (compareWithCoroutinesSuspend && mySuspendCallVisited && opcode == Opcodes.IF_ACMPNE) {
      myContext.removeLastJump();
      mySuspendCallVisited = false;
    }
    myLoadCoroutinesSuspendedVisited = false;
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    super.visitFieldInsn(opcode, owner, name, descriptor);
    // ignore return Unit line
    myHasExecutableCode |= !(owner.equals("kotlin/Unit")
        && name.equals("INSTANCE")
        && descriptor.equals("Lkotlin/Unit;"));
    myStateLabelVisited = opcode == Opcodes.GETFIELD
        && name.equals("label")
        && descriptor.equals("I")
        && owner.startsWith(getClassName());
  }

  /**
   * Ignore TABLESWITCH on coroutine state, which is stored in 'label' field.
   */
  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    super.visitTableSwitchInsn(min, max, dflt, labels);
    myHasExecutableCode = true;
    if (myStateLabelVisited) {
      myContext.removeLastSwitch(dflt, labels);
      if (!myHadLineDataBefore) {
        myContext.getInstrumenter().removeLine(myLine);
      }
    }
    myStateLabelVisited = false;
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    if (opcode == Opcodes.ARETURN && !myHasExecutableCode && !myHadLineDataBefore) {
      myContext.getInstrumenter().removeLine(myLine);
      return;
    }
    myHasExecutableCode = true;
    // ignore generated return on the first line
    if (opcode == Opcodes.ARETURN && myLoadCoroutinesSuspendedVisited && !myHadLineDataBefore) {
      myContext.getInstrumenter().removeLine(myLine);
    }
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    super.visitTypeInsn(opcode, type);
    myHasExecutableCode = true;
  }

  @Override
  public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
    super.visitMultiANewArrayInsn(descriptor, numDimensions);
    myHasExecutableCode = true;
  }

  @Override
  public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    myHasExecutableCode = true;
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    super.visitIntInsn(opcode, operand);
    myHasExecutableCode = true;
  }

  @Override
  public void visitIincInsn(int var, int increment) {
    super.visitIincInsn(var, increment);
    myHasExecutableCode = true;
  }

  @Override
  public void visitLdcInsn(Object value) {
    super.visitLdcInsn(value);
    myHasExecutableCode = true;
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    super.visitLookupSwitchInsn(dflt, keys, labels);
    myHasExecutableCode = true;
  }

  private String getClassName() {
    if (myClassName == null) {
      String name = myContext.getClassName();
      if (name != null) {
        myClassName = name.replace('.', '/');
      }
    }
    return myClassName;
  }
}
