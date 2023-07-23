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

package com.intellij.rt.coverage.instrumentation.filters.branches;

import com.intellij.rt.coverage.instrumentation.InstrumentationUtils;
import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.filters.KotlinUtils;
import com.intellij.rt.coverage.instrumentation.filters.lines.CoverageFilter;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;
import org.jetbrains.coverage.org.objectweb.asm.Type;

/**
 * Filter for Kotlin default args function generated branches.
 * Functions with default args are implemented with another generated function with extra parameters in the end:
 * several masks(each bit means whither this parameter is given or default value should be used) and one Object.
 * One extra if is generated for each default param.
 */
public class KotlinDefaultArgsBranchFilter extends CoverageFilter {
  public static final String DEFAULT_ARGS_SUFFIX = "$default";

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
  public boolean isApplicable(InstrumentationData context) {
    return isFilterApplicable(context);
  }

  public static boolean isFilterApplicable(InstrumentationData data) {
    return (data.getMethodAccess() & Opcodes.ACC_SYNTHETIC) != 0
        && KotlinUtils.isKotlinClass(data)
        && (data.getMethodName().endsWith(DEFAULT_ARGS_SUFFIX) || isConstructorWithDefaultArgs(data.getMethodName(), data.getMethodDesc()));
  }

  private static boolean isConstructorWithDefaultArgs(String name, String desc) {
    return InstrumentationUtils.CONSTRUCTOR.equals(name) && desc != null
        && desc.endsWith("I" + KotlinUtils.KOTLIN_DEFAULT_CONSTRUCTOR_MARKER + ")V");
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
    int[] range = getMaskIndexRange(myContext.getMethodName(), myContext.getMethodDesc());
    myMinMaskIndex = range[0];
    myMaxMaskIndex = range[1];
  }

  public static String getOriginalNameAndDesc(String name, String desc) {
    final Type type = Type.getType(desc);
    final Type[] parameters = type.getArgumentTypes();
    final StringBuilder builder = new StringBuilder();
    if (InstrumentationUtils.CONSTRUCTOR.equals(name)) {
      builder.append(name);
    } else if (name.endsWith(DEFAULT_ARGS_SUFFIX)) {
      builder.append(name, 0, name.length() - DEFAULT_ARGS_SUFFIX.length());
    }
    final int sourceCount = sourceParametersCount(parameters.length);
    builder.append('(');
    for (int i = 0; i < sourceCount; i++) {
      builder.append(parameters[i].getDescriptor());
    }
    builder.append(')');
    builder.append(type.getReturnType().getDescriptor());
    return builder.toString();
  }

  public static int[] getMaskIndexRange(String name, String desc) {
    final Type[] parameters = Type.getType(desc).getArgumentTypes();
    final int sourceCount = sourceParametersCount(parameters.length);
    int size = 0, minIndex = -1;
    for (int i = 0; i < parameters.length - 2; i++) {
      size += parameters[i].getSize();
      if (i == sourceCount - 1) {
        minIndex = size;
      }
    }
    if (InstrumentationUtils.CONSTRUCTOR.equals(name)) {
      // shift as this parameter is at 0 position
      minIndex++;
      size++;
    }
    return new int[]{minIndex, size};
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
        myContext.removeLastJump();
      }
      myIgnoreNextIf = false;
      myAndVisited = false;
    }
  }
}
