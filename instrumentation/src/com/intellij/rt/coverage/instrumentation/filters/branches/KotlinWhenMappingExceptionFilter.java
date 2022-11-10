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

import com.intellij.rt.coverage.data.JumpData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.SwitchData;
import com.intellij.rt.coverage.instrumentation.Instrumenter;
import com.intellij.rt.coverage.instrumentation.kotlin.KotlinUtils;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;

public class KotlinWhenMappingExceptionFilter extends BranchesFilter {
  private Map<Label, PositionData> myJumpLabels;
  private Map<Label, PositionData> mySwitchLabels;
  private Label myCurrentLabel = null;
  private int myCurrentLine;

  @Override
  public boolean isApplicable(Instrumenter context, int access, String name, String desc, String signature, String[] exceptions) {
    return KotlinUtils.isKotlinClass(context);
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    super.visitLineNumber(line, start);
    myCurrentLine = line;
  }

  @Override
  public void visitLabel(Label label) {
    super.visitLabel(label);
    myCurrentLabel = label;
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    if (opcode != Opcodes.GOTO && opcode != Opcodes.JSR) {
      final LineData lineData = myContext.getLineData(myCurrentLine);
      if (lineData != null) {
        if (myJumpLabels == null) myJumpLabels = new HashMap<Label, PositionData>();
        myJumpLabels.put(label, new PositionData(myCurrentLine, lineData.jumpsCount()));
      }
    }
    super.visitJumpInsn(opcode, label);
  }

  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    final LineData lineData = myContext.getLineData(myCurrentLine);
    if (lineData != null) {
      if (mySwitchLabels == null) mySwitchLabels = new HashMap<Label, PositionData>();
      mySwitchLabels.put(dflt, new PositionData(myCurrentLine, lineData.switchesCount()));
    }
    super.visitTableSwitchInsn(min, max, dflt, labels);
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    super.visitTypeInsn(opcode, type);
    if (opcode == Opcodes.NEW && type.equals("kotlin/NoWhenBranchMatchedException")) {
      final PositionData jumpPosition = myJumpLabels == null ? null : myJumpLabels.get(myCurrentLabel);
      if (jumpPosition != null) {
        final LineData lineData = myContext.getLineData(jumpPosition.myLine);
        if (lineData != null && jumpPosition.myIndex < lineData.jumpsCount()) {
          final JumpData jumpData = lineData.getJumpData(jumpPosition.myIndex);
          if (jumpData != null) {
            jumpData.touchFalseHit();
          }
        }
      }
      final PositionData switchPosition = mySwitchLabels == null ? null : mySwitchLabels.get(myCurrentLabel);
      if (switchPosition != null) {
        final LineData lineData = myContext.getLineData(switchPosition.myLine);
        if (lineData != null && switchPosition.myIndex < lineData.switchesCount()) {
          final SwitchData switchData = lineData.getSwitchData(switchPosition.myIndex);
          if (switchData != null) {
            switchData.touch(-1);
          }
        }
      }
    }
  }

  private static class PositionData {
    private final int myLine;
    private final int myIndex;

    private PositionData(int line, int index) {
      myLine = line;
      myIndex = index;
    }
  }
}
