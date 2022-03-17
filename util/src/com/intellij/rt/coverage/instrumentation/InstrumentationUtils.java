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

import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

public class InstrumentationUtils {
  public static final String OBJECT_TYPE = "Ljava/lang/Object;";
  public static final String CLASS_INIT = "<clinit>";
  public static final String CONSTRUCTOR = "<init>";
  public static final String CONSTRUCTOR_DESCRIPTOR = "()V";

  public static void pushInt(MethodVisitor mv, int value) {
    if (value <= Short.MAX_VALUE) {
      mv.visitIntInsn(Opcodes.SIPUSH, value);
    } else {
      mv.visitLdcInsn(value);
    }
  }

  /**
   * Util method for int array instrumentation.
   * Stack must be: array, index.
   * Generates code <code>array[index]++</code>.
   */
  public static void incrementIntArrayByIndex(MethodVisitor mv) {
    mv.visitInsn(Opcodes.DUP2);
    //load array[index]
    mv.visitInsn(Opcodes.IALOAD);

    //increment
    mv.visitInsn(Opcodes.ICONST_1);
    mv.visitInsn(Opcodes.IADD);

    //stack: array, index, incremented value: store value in array[index]
    mv.visitInsn(Opcodes.IASTORE);
  }

  public static int getBytecodeVersion(ClassReader cr) {
    return cr.readInt(4) & 0xFFFF;
  }
}
