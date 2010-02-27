package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.LineData;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LineEnumerator extends MethodAdapter implements Opcodes {
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
  private Set myJumps;
  private Map mySwitches;

  private final MethodVisitor myWriterMethodVisitor;
  private final boolean myIsReferencedType;
  private boolean myRemoveNotNullJumps;

  private static final byte SEEN_NOTHING = 0;
  private static final byte ILOAD_SEEN = 1;
  private static final byte IFNE_SEEN = 2;
  private static final byte ICONST_1_SEEN = 3;
  private static final byte GOTO_SEEN = 4;

  private static final byte GETSTATIC_SEEN = 5;

  private byte myState = SEEN_NOTHING;

  private boolean myHasInstructions;

  public LineEnumerator(ClassInstrumenter classInstrumenter, final MethodVisitor mv,
                        final int access,
                        final String name,
                        final String desc,
                        final String signature,
                        final String[] exceptions) {
    super(new MethodNode(access, name, desc, signature, exceptions));
    myClassInstrumenter = classInstrumenter;
    myWriterMethodVisitor = mv;
    myAccess = access;
    myMethodName = name;
    mySignature = desc;
    methodNode = (MethodNode)this.mv;
    final Type returnType = Type.getReturnType(desc);
    myIsReferencedType = returnType.getSort() == Type.OBJECT || returnType.getSort() == Type.ARRAY;
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
    if (opcode != GOTO && opcode != JSR && !myMethodName.equals("<clinit>")) {
      if (myJumps == null) myJumps = new HashSet();
      myJumps.add(label);
      myLastJump = label;
      final LineData lineData = myClassInstrumenter.getLineData(myCurrentLine);
      if (lineData != null) {
        lineData.addJump(myCurrentJump++);
      }
    }
    if (myState == GETSTATIC_SEEN && opcode == IFNE) {
      myState = SEEN_NOTHING;
      final LineData lineData = myClassInstrumenter.getLineData(myCurrentLine);
      if (lineData != null && isJump(label)) {
        lineData.removeJump(myCurrentJump--);
        myJumps.remove(myLastJump);
        myLastJump = null;
      }
    }
    if (myState == ILOAD_SEEN && opcode == IFNE) {
      myState = IFNE_SEEN;
    }
    else if (myState == ICONST_1_SEEN && opcode == GOTO) {
      myState = GOTO_SEEN;
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
    if (mySwitches == null) mySwitches = new HashMap();
    mySwitches.put(dflt, new Integer(-1));
    for (int i = labels.length - 1; i >= 0; i--) {
      mySwitches.put(labels[i], new Integer(i));
    }
  }


  public Integer getSwitchKey(Label label) {
    if (mySwitches == null) return null;
    return (Integer)mySwitches.get(label);
  }

  public String getMethodName() {
    return myMethodName;
  }

  public void visitInsn(final int opcode) {
    super.visitInsn(opcode);
    if (!myHasExecutableLines) return;
    //remove } lines from coverage report
    if (opcode == RETURN && !myHasInstructions) {
      myClassInstrumenter.removeLine(myCurrentLine);
    } else {
      myHasInstructions = true;
    }

    //remove previous jump -> which is wrapper to throw IllegalStateException("method can't return null")
    if (myRemoveNotNullJumps && opcode == ARETURN) {
      final LineData lineData = myClassInstrumenter.getLineData(myCurrentLine);
      if (lineData != null) {
        lineData.removeJump(myCurrentJump--);
        myJumps.remove(myLastJump);
      }
    }
    if (opcode == ICONST_1 && myState == IFNE_SEEN) {
      myState = ICONST_1_SEEN;
    }
    else if (opcode == ICONST_0 && myState == GOTO_SEEN) {
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
    if (opcode == ILOAD) {
      myState = ILOAD_SEEN;
    } else {
      myState = SEEN_NOTHING;
    }
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
      myState = GETSTATIC_SEEN;
    } else {
      myState = SEEN_NOTHING;
    }
    myHasInstructions = true;
  }

  public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc) {
    super.visitMethodInsn(opcode, owner, name, desc);
    if (!myHasExecutableLines) return;
    myState = SEEN_NOTHING;
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

  public AnnotationVisitor visitAnnotation(final String anno, final boolean visible) {
    final AnnotationVisitor visitor = super.visitAnnotation(anno, visible);
    if (!myHasExecutableLines) return visitor;
    if (myIsReferencedType && anno.equals("Lorg/jetbrains/annotations/NotNull;")) {
      myRemoveNotNullJumps = true;
    }
    return visitor;
  }
}
