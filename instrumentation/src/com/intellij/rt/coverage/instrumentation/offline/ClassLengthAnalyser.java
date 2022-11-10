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

package com.intellij.rt.coverage.instrumentation.offline;

import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * A class visitor to collect information about number of jumps and lines.
 */
class ClassLengthAnalyser extends ClassVisitor {
  private int myMaxLine = -1;
  private int myHits = 0;

  public ClassLengthAnalyser() {
    super(Opcodes.API_VERSION);
  }

  public int getHits() {
    return myHits;
  }

  public int getMaxLine() {
    return myMaxLine;
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    final MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
    return new MethodVisitor(Opcodes.API_VERSION, mv) {
      @Override
      public void visitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
        myHits += 2;
      }

      @Override
      public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        myHits += 1 + labels.length;
      }

      @Override
      public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        myHits += 1 + labels.length;
      }

      @Override
      public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        if (line > myMaxLine) myMaxLine = line;
        myHits++;
      }
    };
  }
}
