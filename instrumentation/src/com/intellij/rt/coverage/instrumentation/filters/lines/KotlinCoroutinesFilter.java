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

package com.intellij.rt.coverage.instrumentation.filters.lines;

import com.intellij.rt.coverage.instrumentation.InstrumentationUtils;
import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.data.Key;
import com.intellij.rt.coverage.instrumentation.filters.KotlinUtils;
import com.intellij.rt.coverage.instrumentation.filters.branches.KotlinDefaultArgsBranchFilter;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;
import org.jetbrains.coverage.org.objectweb.asm.Type;

/**
 * Filter out generated Kotlin coroutines state machines.
 * Namely, TABLESWITCH on state and 'if suspend' checks are ignored.
 * <p>
 * Coroutines call markers:
 * <ol>
 *   <li>COROUTINE_SUSPENDED loaded</li>
 *   <ol>
 *     <li>It could be loaded via <code>IntrinsicsKt.getCOROUTINE_SUSPENDED</code> call</li>
 *     <li>Or it could be stored to a local variable</li>
 *   </ol>
 *   <li>Suspend call</li>
 *   <ol>
 *     <li>It could be a call with <code>Lkotlin/coroutines/Continuation;</code> last parameter and <code>Object</code> return type</li>
 *     <li>Or it could be a default args call, then it also has default arguments call type suffix</li>
 *     <li>Or suspend lambda call via <code>invoke</code> method</li>
 *   </ol>
 *   <li>Label access</li>
 *   <ol>
 *     <li>Label is stored as a field. It contains the information to resume the coroutine after suspension</li>
 *   </ol>
 * </ol>
 * We ignore:
 * <ol>
 *   <li>IF_ACMPNE jump, if it happens after coroutines call and compares its result with COROUTINE_SUSPENDED</li>
 *   <li>TABLESWITCH, if it switches on coroutine label</li>
 *   <li>ARETURN, if it is generated on the first line</li>
 *   <li>Generated exception throwing</li>
 * </ol>
 * <p>
 * States for ignoring exception:
 * <ol>
 *   <li>new IllegalStateException</li>
 *   <li>DUP</li>
 *   <li>LDC exception message</li>
 *   <li>INVOKESPECIAL exception constructor</li>
 *   <li>ATHROW</li>
 * </ol>
 */
public class KotlinCoroutinesFilter extends CoverageFilter {
  private boolean myGetCoroutinesSuspendedVisited = false;
  private boolean myStoreCoroutinesSuspendedVisited = false;
  private boolean myLoadCoroutinesSuspendedVisited = false;
  private boolean myLoadStateLabelVisited = false;
  private boolean mySuspendCallVisited = false;
  private int myCoroutinesSuspendedIndex = -1;
  private int myLine = -1;
  private boolean myHadLineDataBefore = false;

  private int myState = 0;
  private boolean myHasInstructions;

  /**
   * This filter is applicable for suspend methods of Kotlin class.
   * It could be a suspend lambda, then it's name is 'invokeSuspend'.
   * Or it could be a suspend method, then it's last parameter is a Continuation.
   */
  @Override
  public boolean isApplicable(InstrumentationData context) {
    return KotlinUtils.isKotlinClass(context)
        && (context.getMethodName().equals("invokeSuspend") || context.getMethodDesc().endsWith("Lkotlin/coroutines/Continuation;)" + InstrumentationUtils.OBJECT_TYPE));
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    myHadLineDataBefore = myContext.getLineData(line) != null;
    super.visitLineNumber(line, start);
    myLine = line;
    myHasInstructions = false;
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

    if (myState == 3 && opcode == Opcodes.INVOKESPECIAL
        && "java/lang/IllegalStateException".equals(owner)
        && "<init>".equals(name) && "(Ljava/lang/String;)V".equals(descriptor)) {
      myState = 4;
    } else {
      myState = 0;
      myHasInstructions = true;
    }

    boolean getCoroutinesSuspendedVisited = opcode == Opcodes.INVOKESTATIC
        && owner.equals("kotlin/coroutines/intrinsics/IntrinsicsKt")
        && name.equals("getCOROUTINE_SUSPENDED")
        && descriptor.equals("()" + InstrumentationUtils.OBJECT_TYPE);
    String originalDesc = descriptor;
    if (name.endsWith(KotlinDefaultArgsBranchFilter.DEFAULT_ARGS_SUFFIX)) {
      String desc = KotlinDefaultArgsBranchFilter.getOriginalNameAndDesc(name, descriptor);
      int index = desc.indexOf('(');
      if (index > 0) {
        originalDesc = desc.substring(index);
      }
    }
    boolean suspendCallVisited = originalDesc.endsWith("Lkotlin/coroutines/Continuation;)" + InstrumentationUtils.OBJECT_TYPE)
        || owner.startsWith("kotlin/jvm/functions/Function")
        && name.equals("invoke") && opcode == Opcodes.INVOKEINTERFACE
        && descriptor.endsWith(")" + InstrumentationUtils.OBJECT_TYPE);
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
    myHasInstructions = true;
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
    myHasInstructions = true;
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
    myHasInstructions = true;
    final boolean labelVisited = name.equals("label")
        && descriptor.equals("I")
        && Type.getObjectType(owner).getClassName().startsWith(myContext.get(Key.CLASS_NAME));
    myLoadStateLabelVisited = labelVisited && opcode == Opcodes.GETFIELD;
  }

  /**
   * Ignore TABLESWITCH on coroutine state, which is stored in 'label' field.
   */
  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    super.visitTableSwitchInsn(min, max, dflt, labels);
    myHasInstructions = true;
    if (myLoadStateLabelVisited) {
      myContext.removeLastSwitch(dflt, labels);
      if (!myHadLineDataBefore) {
        onIgnoredLine(myLine);
      }
    }
    myLoadStateLabelVisited = false;
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    if (myState == 1 && opcode == Opcodes.DUP) {
      myState = 2;
      return;
    } else if (myState == 4 && opcode == Opcodes.ATHROW) {
      if (!myHadLineDataBefore && !myHasInstructions) {
        onIgnoredLine(myLine);
      }
      return;
    }
    myHasInstructions = true;
    myState = 0;
    // ignore generated return on the first line
    if (opcode == Opcodes.ARETURN && myLoadCoroutinesSuspendedVisited && !myHadLineDataBefore) {
      onIgnoredLine(myLine);
    }
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    super.visitTypeInsn(opcode, type);
    if (opcode == Opcodes.NEW && "java/lang/IllegalStateException".equals(type)) {
      myState = 1;
    } else {
      myHasInstructions = true;
      myState = 0;
    }
  }

  @Override
  public void visitLdcInsn(Object value) {
    super.visitLdcInsn(value);
    if (myState == 2 && "call to 'resume' before 'invoke' with coroutine".equals(value)) {
      myState = 3;
    } else {
      myHasInstructions = true;
      myState = 0;
    }
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    super.visitLookupSwitchInsn(dflt, keys, labels);
    myHasInstructions = true;
    myState = 0;
  }

  @Override
  public void visitIincInsn(int var, int increment) {
    super.visitIincInsn(var, increment);
    myHasInstructions = true;
    myState = 0;
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    super.visitIntInsn(opcode, operand);
    myHasInstructions = true;
    myState = 0;
  }

  @Override
  public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
    super.visitMultiANewArrayInsn(descriptor, numDimensions);
    myHasInstructions = true;
    myState = 0;
  }

  private void onIgnoredLine(int line) {
    myContext.removeLine(line);
  }
}
