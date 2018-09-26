/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.util.ClassNameUtil;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;
import org.jetbrains.coverage.org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LineEnumerator extends MethodVisitor implements Opcodes {
  private final ClassInstrumenter myClassInstrumenter;
  private final int myAccess;
  private final String myMethodName;
  private final String mySignature;
  private final MethodNode methodNode;

  private int myCurrentLine;
  private int myCurrentJump;
  private int myCurrentSwitch;

  private Label myLastJump;

  private boolean myHasExecutableLines = false;
  private Set<Label> myJumps;
  private Map<Label, Integer> mySwitches;

  private final MethodVisitor myWriterMethodVisitor;

  private static final byte SEEN_NOTHING = 0;

  /**
  * DUP
  * IFNONNULL
  * ICONST/BIPUSH
  * INVOKESTATIC className.$$$reportNull$$$0 (I)V 
  */
  private static final byte DUP_SEEN = 1;
  private static final byte IFNONNULL_SEEN = 2;
  private static final byte PARAM_CONST_SEEN = 3;

  private static final byte ASSERTIONS_DISABLED_STATE = 5;

  private byte myState = SEEN_NOTHING;

  private boolean myHasInstructions;

  public LineEnumerator(ClassInstrumenter classInstrumenter, final MethodVisitor mv,
                        final int access,
                        final String name,
                        final String desc,
                        final String signature,
                        final String[] exceptions) {
    super(Opcodes.ASM6, new MethodNode(access, name, desc, signature, exceptions));
    myClassInstrumenter = classInstrumenter;
    myWriterMethodVisitor = mv;
    myAccess = access;
    myMethodName = name;
    mySignature = desc;
    methodNode = (MethodNode)this.mv;
  }


  public void visitEnd() {
    super.visitEnd();
    methodNode.accept(!myHasExecutableLines ? myWriterMethodVisitor : new TouchCounter(this, myAccess, mySignature));
  }


  public void visitLineNumber(int line, Label start) {
    super.visitLineNumber(line, start);
    myHasInstructions = false;
    myCurrentLine = line;
    myCurrentJump = 0;
    myCurrentSwitch = 0;
    myHasExecutableLines = true;
    myClassInstrumenter.getOrCreateLineData(myCurrentLine, myMethodName, mySignature);
  }


  public String getClassName() {
    return myClassInstrumenter.getClassName();
  }

  public MethodVisitor getWV() {
    return myWriterMethodVisitor;
  }

  public void visitJumpInsn(final int opcode, final Label label) {
    if (!myHasExecutableLines) {
      super.visitJumpInsn(opcode, label);
      return;
    }
    if (opcode != Opcodes.GOTO && opcode != Opcodes.JSR && !myMethodName.equals("<clinit>")) {
      if (myJumps == null) myJumps = new HashSet<Label>();
      myJumps.add(label);
      myLastJump = label;
      final LineData lineData = myClassInstrumenter.getLineData(myCurrentLine);
      if (lineData != null) {
        lineData.addJump(myCurrentJump++);
      }
    }
    if (myState == ASSERTIONS_DISABLED_STATE && opcode == Opcodes.IFNE) {
      myState = SEEN_NOTHING;
      final LineData lineData = myClassInstrumenter.getLineData(myCurrentLine);
      if (lineData != null && isJump(label)) {
        lineData.removeJump(myCurrentJump--);
        myJumps.remove(myLastJump);
        myLastJump = null;
      }
    }

    if (myState == DUP_SEEN && opcode == Opcodes.IFNONNULL) {
      myState = IFNONNULL_SEEN;
    }
    else {
      myState = SEEN_NOTHING;
    }
    myHasInstructions = true;
    super.visitJumpInsn(opcode, label);
  }

  public boolean isJump(Label jump) {
    return myJumps != null && myJumps.contains(jump);
  }


  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    super.visitLookupSwitchInsn(dflt, keys, labels);
    if (!myHasExecutableLines) return;
    rememberSwitchLabels(dflt, labels);
    final LineData lineData = myClassInstrumenter.getLineData(myCurrentLine);
    if (lineData != null) {
      lineData.addSwitch(myCurrentSwitch++, keys);
    }
    myState = SEEN_NOTHING;
    myHasInstructions = true;
  }

  public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
    super.visitTableSwitchInsn(min, max, dflt, labels);
    if (!myHasExecutableLines) return;
    rememberSwitchLabels(dflt, labels);
    final LineData lineData = myClassInstrumenter.getLineData(myCurrentLine);
    if (lineData != null) {
      lineData.addSwitch(myCurrentSwitch++, min, max);
    }
    myState = SEEN_NOTHING;
    myHasInstructions = true;
  }

  private void rememberSwitchLabels(final Label dflt, final Label[] labels) {
    if (mySwitches == null) mySwitches = new HashMap<Label, Integer>();
    mySwitches.put(dflt, -1);
    for (int i = labels.length - 1; i >= 0; i--) {
      mySwitches.put(labels[i], i);
    }
  }


  public Integer getSwitchKey(Label label) {
    if (mySwitches == null) return null;
    return mySwitches.get(label);
  }

  public String getMethodName() {
    return myMethodName;
  }

  public void visitInsn(final int opcode) {
    super.visitInsn(opcode);
    if (!myHasExecutableLines) return;
    //remove } lines from coverage report
    if (opcode == Opcodes.RETURN && !myHasInstructions) {
      myClassInstrumenter.removeLine(myCurrentLine);
    } else {
      myHasInstructions = true;
    }

    if (opcode == Opcodes.DUP) {
      myState = DUP_SEEN;
    } 
    else if (myState == IFNONNULL_SEEN && 
        (opcode >= Opcodes.ICONST_0 && opcode <= Opcodes.ICONST_5 || opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH)) {
      myState = PARAM_CONST_SEEN;
    }
    else {
      myState = SEEN_NOTHING;
    }
  }

  public void visitIntInsn(final int opcode, final int operand) {
    super.visitIntInsn(opcode, operand);
    if (!myHasExecutableLines) return;
    myState = SEEN_NOTHING;
    myHasInstructions = true;
  }

  public void visitVarInsn(final int opcode, final int var) {
    super.visitVarInsn(opcode, var);
    if (!myHasExecutableLines) return;
    myState = SEEN_NOTHING;
    myHasInstructions = true;
  }

  public void visitTypeInsn(final int opcode, final String type) {
    super.visitTypeInsn(opcode, type);
    if (!myHasExecutableLines) return;
    myState = SEEN_NOTHING;
    myHasInstructions = true;
  }

  public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
    super.visitFieldInsn(opcode, owner, name, desc);
    if (!myHasExecutableLines) return;
    if (opcode == Opcodes.GETSTATIC && name.equals("$assertionsDisabled")) {
      myState = ASSERTIONS_DISABLED_STATE;
    }
    else {
      myState = SEEN_NOTHING;
    }
    myHasInstructions = true;
  }

  public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
    super.visitMethodInsn(opcode, owner, name, desc, itf);
    if (!myHasExecutableLines) return;

    if (myState == PARAM_CONST_SEEN && 
        opcode == Opcodes.INVOKESTATIC &&
        name.startsWith("$$$reportNull$$$") &&
        ClassNameUtil.convertToFQName(owner).equals(myClassInstrumenter.getClassName())) {
      final LineData lineData = myClassInstrumenter.getLineData(myCurrentLine);
      if (lineData != null) {
        lineData.removeJump(myCurrentJump--);
        myJumps.remove(myLastJump);
      }
      myState = SEEN_NOTHING;
    }
    else {
      myState = SEEN_NOTHING;
    }
    myHasInstructions = true;
  }

  public void visitLdcInsn(final Object cst) {
    super.visitLdcInsn(cst);
    if (!myHasExecutableLines) return;
    myState = SEEN_NOTHING;
    myHasInstructions = true;
  }

  public void visitIincInsn(final int var, final int increment) {
    super.visitIincInsn(var, increment);
    if (!myHasExecutableLines) return;
    myState = SEEN_NOTHING;
    myHasInstructions = true;
  }

  public void visitMultiANewArrayInsn(final String desc, final int dims) {
    super.visitMultiANewArrayInsn(desc, dims);
    if (!myHasExecutableLines) return;
    myState = SEEN_NOTHING;
    myHasInstructions = true;
  }
}
