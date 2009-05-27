package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.ProjectData;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class TouchCounter extends MethodAdapter implements Opcodes {
  private final int myVariablesCount;

  private final LineEnumerator myEnumerator;

  private Label myStartLabel;
  private Label myEndLabel;

  private int myCurrentLine;
  private int myCurrentJumpIdx;
  private int myCurrentSwitchIdx;

  private int myLastJump = -1;
  private int myLastLineJump = -1;

  private static final byte SEEN_NOTHING = 0;
  private static final byte GETSTATIC_SEEN = 1;

  private byte myState;

  public TouchCounter(final LineEnumerator enumerator, int access, String desc) {
    super(enumerator.getWV());
    myEnumerator = enumerator;
    int variablesCount = ((ACC_STATIC & access) != 0) ? 0 : 1;
    final Type[] args = Type.getArgumentTypes(desc);
    for (int i = 0; i < args.length; i++) {
      variablesCount += args[i].getSize();
    }
    myVariablesCount = variablesCount;
  }


  public void visitLineNumber(int line, Label start) {
    myCurrentLine = line;
    myCurrentJumpIdx = 0;
    myCurrentSwitchIdx = 0;

    loadClassData();
    mv.visitIntInsn(SIPUSH, line);
    mv.visitMethodInsn(INVOKEVIRTUAL, ClassData.CLASS_DATA_OWNER, "touch", "(I)V");

    mv.visitFieldInsn(GETSTATIC, ProjectData.PROJECT_DATA_OWNER, "ourProjectData", ProjectData.PROJECT_DATA_TYPE);
    mv.visitLdcInsn(myEnumerator.getClassName());
    mv.visitIntInsn(SIPUSH, line);
    mv.visitMethodInsn(INVOKEVIRTUAL, ProjectData.PROJECT_DATA_OWNER, "trace", "(Ljava/lang/String;I)V");

    super.visitLineNumber(line, start);
  }

  private void loadClassData() {
    mv.visitFieldInsn(GETSTATIC, myEnumerator.getClassName().replace('.', '/'), "__class__data__", ClassData.CLASS_DATA_TYPE);
  }

  public void visitLabel(Label label) {
    if (myStartLabel == null) {
      myStartLabel = label;
    }
    myEndLabel = label;

    super.visitLabel(label);


    final boolean isJump = myEnumerator.isJump(label);
    if (myLastJump != -1) {
      Label l = new Label();

      mv.visitVarInsn(ILOAD, getLineVariableNumber());
      mv.visitIntInsn(SIPUSH, myLastLineJump);
      mv.visitJumpInsn(IF_ICMPNE, l);

      mv.visitVarInsn(ILOAD, getJumpVariableNumber());
      mv.visitIntInsn(SIPUSH, myLastJump);
      mv.visitJumpInsn(IF_ICMPNE, l);

      touchLastJump();

      if (isJump) {
        Label l1 = new Label();
        mv.visitJumpInsn(GOTO, l1);
        mv.visitLabel(l);
        mv.visitVarInsn(ILOAD, getJumpVariableNumber());
        mv.visitJumpInsn(IFLT, l1);
        touchBranch(true);
        mv.visitLabel(l1);
      }
      else {
        mv.visitLabel(l);
      }
    }
    else if (isJump) {
      mv.visitVarInsn(ILOAD, getJumpVariableNumber());
      Label newLabelX = new Label();
      mv.visitJumpInsn(IFLT, newLabelX);
      touchBranch(true);
      mv.visitLabel(newLabelX);
    }


    final Integer key = myEnumerator.getSwitchKey(label);
    if (key != null) {
      loadClassData();
      mv.visitVarInsn(ILOAD, getLineVariableNumber());
      mv.visitVarInsn(ILOAD, getSwitchVariableNumber());
      mv.visitIntInsn(SIPUSH, key.intValue());
      mv.visitMethodInsn(INVOKEVIRTUAL, ClassData.CLASS_DATA_OWNER, "touch", "(III)V");
    }
  }

  private void touchBranch(final boolean trueHit) {
    loadClassData();
    mv.visitVarInsn(ILOAD, getLineVariableNumber());
    mv.visitVarInsn(ILOAD, getJumpVariableNumber());
    mv.visitInsn(trueHit ? ICONST_0 : ICONST_1);
    mv.visitMethodInsn(INVOKEVIRTUAL, ClassData.CLASS_DATA_OWNER, "touch", "(IIZ)V");

    mv.visitIntInsn(SIPUSH, -1);
    mv.visitVarInsn(ISTORE, getJumpVariableNumber());
  }

  private void touchLastJump() {
    if (myLastJump != -1) {
      myLastJump = -1;
      touchBranch(false);
    }
    myState = SEEN_NOTHING;
  }


  public void visitJumpInsn(final int opcode, final Label label) {
    byte state = myState;
    touchLastJump();
    if (opcode != GOTO && opcode != JSR && !myEnumerator.getMethodName().equals("<clinit>") && myEnumerator.isJump(label) && !(state == GETSTATIC_SEEN && opcode == IFNE)) {
      myLastJump = myCurrentJumpIdx;
      myLastLineJump = myCurrentLine;
      mv.visitIntInsn(SIPUSH, myCurrentLine);
      mv.visitVarInsn(ISTORE, getLineVariableNumber());
      mv.visitIntInsn(SIPUSH, myCurrentJumpIdx++);
      mv.visitVarInsn(ISTORE, getJumpVariableNumber());
    }
    super.visitJumpInsn(opcode, label);
  }


  public void visitCode() {
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, getLineVariableNumber());

    mv.visitIntInsn(SIPUSH, -1);
    mv.visitVarInsn(ISTORE, getJumpVariableNumber());

    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ISTORE, getSwitchVariableNumber());

    if (myEnumerator.isStaticInitializer()) {
      Instrumenter.createClassDataField(mv, myEnumerator.getClassName());
    }

    super.visitCode();
  }

  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    touchLastJump();
    storeSwitchDescriptor();
    super.visitLookupSwitchInsn(dflt, keys, labels);
  }

  public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
    touchLastJump();
    storeSwitchDescriptor();
    super.visitTableSwitchInsn(min, max, dflt, labels);
  }

  private void storeSwitchDescriptor() {
    mv.visitIntInsn(SIPUSH, myCurrentLine);
    mv.visitVarInsn(ISTORE, getLineVariableNumber());

    mv.visitIntInsn(SIPUSH, myCurrentSwitchIdx++);
    mv.visitVarInsn(ISTORE, getSwitchVariableNumber());
  }


  public void visitFieldInsn(int opcode, String owner, String name, String desc) {
    touchLastJump();
    if (opcode == GETSTATIC && name.equals("$assertionsDisabled")) {
      myState = GETSTATIC_SEEN;
    }
    super.visitFieldInsn(opcode, owner, name, desc);
  }


  public void visitInsn(int opcode) {
    touchLastJump();
    super.visitInsn(opcode);
  }

  public void visitIntInsn(int opcode, int operand) {
    touchLastJump();
    super.visitIntInsn(opcode, operand);
  }

  public void visitLdcInsn(Object cst) {
    touchLastJump();
    super.visitLdcInsn(cst);
  }

  public void visitMultiANewArrayInsn(String desc, int dims) {
    touchLastJump();
    super.visitMultiANewArrayInsn(desc, dims);
  }


  public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
    touchLastJump();
    super.visitTryCatchBlock(start, end, handler, type);
  }

  public void visitTypeInsn(int opcode, String desc) {
    touchLastJump();
    super.visitTypeInsn(opcode, desc);
  }

  public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc) {
    touchLastJump();
    super.visitMethodInsn(opcode, owner, name, desc);
  }


  public void visitVarInsn(int opcode, int var) {
    touchLastJump();
    mv.visitVarInsn(opcode, adjustVariable(var));
  }

  public void visitIincInsn(int var, int increment) {
    touchLastJump();
    mv.visitIincInsn(adjustVariable(var), increment);
  }

  public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
    touchLastJump();
    mv.visitLocalVariable(name, desc, signature, start, end, adjustVariable(index));
  }

  private int adjustVariable(final int var) {
    return (var >= getLineVariableNumber()) ? var + 3 : var;
  }

  public int getLineVariableNumber() {
    return myVariablesCount;
  }

  private int getJumpVariableNumber() {
    return myVariablesCount + 1;
  }

  private int getSwitchVariableNumber() {
    return myVariablesCount + 2;
  }


  public void visitMaxs(int maxStack, int maxLocals) {
    if (myStartLabel != null && myEndLabel != null) {
      mv.visitLocalVariable("__line__number__", "I", null, myStartLabel, myEndLabel, getLineVariableNumber());
      mv.visitLocalVariable("__jump__number__", "I", null, myStartLabel, myEndLabel, getJumpVariableNumber());
      mv.visitLocalVariable("__switch__number__", "I", null, myStartLabel, myEndLabel, getSwitchVariableNumber());
    }
    super.visitMaxs(maxStack, maxLocals);
  }
}
