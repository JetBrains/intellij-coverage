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

package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.util.OptionsUtil;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

public class InstrumentationUtils {
  public static final String OBJECT_TYPE = "Ljava/lang/Object;";
  public static final String CLASS_INIT = "<clinit>";
  public static final String CONSTRUCTOR = "<init>";
  public static final String CONSTRUCTOR_DESCRIPTOR = "()V";

  /**
   * Util method for touching coverage counter which is stored in an int array.
   * This method has different strategies basing on <code>OptionsUtil.CALCULATE_HITS_COUNT</code> option:
   * when option is true, array counter is incremented,
   * otherwise the value is just set to 1.
   *
   * @param mv current method visitor
   * @param lv local variable index to load int array
   * @param id index of a hit in the array
   */
  public static void touchById(MethodVisitor mv, int lv, int id) {
    mv.visitVarInsn(Opcodes.ALOAD, lv);
    InstrumentationUtils.pushInt(mv, id);

    if (OptionsUtil.CALCULATE_HITS_COUNT) {
      mv.visitInsn(Opcodes.DUP2);
      // load array[index]
      mv.visitInsn(Opcodes.IALOAD);

      // increment
      mv.visitInsn(Opcodes.ICONST_1);
      mv.visitInsn(Opcodes.IADD);

      // stack: array, index, incremented value: store value in array[index]
      mv.visitInsn(Opcodes.IASTORE);
    } else {
      mv.visitInsn(Opcodes.ICONST_1);
      mv.visitInsn(Opcodes.IASTORE);
    }
  }

  public static int getBytecodeVersion(ClassReader cr) {
    return cr.readInt(4) & 0xFFFF;
  }

  public static void pushInt(MethodVisitor mv, int value) {
    if (value >= -1 && value <= 5) {
      mv.visitInsn(Opcodes.ICONST_0 + value);
    } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
      mv.visitIntInsn(Opcodes.BIPUSH, value);
    } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
      mv.visitIntInsn(Opcodes.SIPUSH, value);
    } else {
      mv.visitLdcInsn(value);
    }
  }

  /**
   * Push value on stack.
   *
   * @param o value to push must be primitive or String
   */
  public static void push(MethodVisitor mv, Object o) {
    if (o == null) {
      mv.visitInsn(Opcodes.ACONST_NULL);
    } else if (o instanceof Integer) {
      pushInt(mv, (Integer) o);
    } else if (o instanceof Boolean) {
      pushInt(mv, ((Boolean) o) ? 1 : 0);
    } else if (o instanceof Double || o instanceof Float || o instanceof Long || o instanceof String) {
      mv.visitLdcInsn(o);
    } else {
      throw new IllegalArgumentException("Cannot push element of type " + o.getClass());
    }
  }
}
