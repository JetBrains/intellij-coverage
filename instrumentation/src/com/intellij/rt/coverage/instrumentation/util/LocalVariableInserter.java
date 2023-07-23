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

package com.intellij.rt.coverage.instrumentation.util;

import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;
import org.jetbrains.coverage.org.objectweb.asm.Type;
import org.jetbrains.coverage.org.objectweb.asm.commons.LocalVariablesSorter;

/**
 * This method visitor inserts a variable into a method.
 * Use {@link LocalVariableInserter#getLVIndex()} to clarify inserted variable index.
 */
public class LocalVariableInserter extends LocalVariablesSorter {
  private final String myVariableName;
  private final String myVariableType;
  private int myVariableIndex = -1;
  private Label myStartLabel;
  private Label myEndLabel;

  public LocalVariableInserter(MethodVisitor methodVisitor, int access, String descriptor, String variableName, String variableType) {
    super(Opcodes.API_VERSION, access, descriptor, methodVisitor);
    myVariableName = variableName;
    myVariableType = variableType;
  }

  public void visitLabel(Label label) {
    if (myStartLabel == null) {
      myStartLabel = label;
    }
    myEndLabel = label;
    super.visitLabel(label);
  }

  public void visitMaxs(int maxStack, int maxLocals) {
    if (myStartLabel != null && myEndLabel != null) {
      mv.visitLocalVariable(myVariableName, myVariableType, null, myStartLabel, myEndLabel, getLVIndex());
    }
    super.visitMaxs(maxStack, maxLocals);
  }

  public int getLVIndex() {
    if (myVariableIndex == -1) {
      myVariableIndex = newLocal(Type.getType(myVariableType));
    }
    return myVariableIndex;
  }

  public void loadFromLocal() {
    mv.visitVarInsn(Opcodes.ALOAD, getLVIndex());
  }
}
