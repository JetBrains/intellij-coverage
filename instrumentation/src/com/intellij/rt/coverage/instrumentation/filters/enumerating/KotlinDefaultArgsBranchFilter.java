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
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;
import org.jetbrains.coverage.org.objectweb.asm.Type;

/**
 * Filter for Kotlin default args function generated branches.
 * Functions with default args are implemented with another generated function with extra parameters in the end:
 * several masks(each bit means whither this parameter is given or default value should be used) and one Object.
 * One extra if is generated for each default param.
 */
public class KotlinDefaultArgsBranchFilter extends LineEnumeratorFilter {

  /**
   * Index of first mask variable.
   */
  private int myMaxMaskIndex = -1;

  /**
   * Index of last mask variable.
   */
  private int myMinMaskIndex = -1;

  private boolean myIgnoreNextIf = false;
  private boolean myAndVisited = false;

  @Override
  public boolean isApplicable(Instrumenter context, int access, String name, String desc, String signature, String[] exceptions) {
    return (access & Opcodes.ACC_SYNTHETIC) != 0
        && KotlinUtils.isKotlinClass(context)
        && name.endsWith("$default");
  }


  /**
   * Find the number of parameters in the source method.
   * <p>
   * If there are k params in the source method then in the modified method there are
   * z = f(k) = k + 1 + ceil(k / 32) parameters as several int flags and one Object are added as parameters.
   * This is the inverse function of f.
   *
   * @param z the number of parameters in the modified method
   * @return the number of parameters in the source method
   */
  static int sourceParametersCount(int z) {
    return z - 1 - (z - 1 + 32) / 33;
  }

  @Override
  public void visitCode() {
    super.visitCode();
    Type[] parameters = Type.getType(myContext.getDescriptor()).getArgumentTypes();
    int sourceCount = sourceParametersCount(parameters.length);
    int size = 0;
    for (int i = 0; i < parameters.length - 2; i++) {
      size += parameters[i].getSize();
      if (i == sourceCount - 1) {
        myMinMaskIndex = size;
      }
    }
    myMaxMaskIndex = size;
  }

  @Override
  public void visitVarInsn(int opcode, int var) {
    super.visitVarInsn(opcode, var);
    myIgnoreNextIf = myMinMaskIndex <= var && var <= myMaxMaskIndex;
  }

  @Override
  public void visitInsn(int opcode) {
    super.visitInsn(opcode);
    myAndVisited |= myIgnoreNextIf && opcode == Opcodes.IAND;
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    super.visitJumpInsn(opcode, label);
    if (opcode == Opcodes.IFEQ && myIgnoreNextIf) {
      if (myAndVisited) {
        myContext.getBranchData().removeLastJump();
      }
      myIgnoreNextIf = false;
      myAndVisited = false;
    }
  }
}
