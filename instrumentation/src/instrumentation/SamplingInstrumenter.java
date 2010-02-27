package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.util.LinesUtil;
import com.intellij.rt.coverage.data.ProjectData;
import org.objectweb.asm.*;

public class SamplingInstrumenter extends Instrumenter {
  private int mySize;
  private static final String OBJECT_TYPE = "Ljava/lang/Object;";

  public SamplingInstrumenter(final ProjectData projectData, ClassVisitor classVisitor) {
    super(projectData, classVisitor);
  }

  protected MethodVisitor createMethodLineEnumerator(final MethodVisitor mv,
                                                     final String name,
                                                     final String desc,
                                                     final int access,
                                                     final String signature,
                                                     final String[] exceptions) {
    int variablesCount = ((Opcodes.ACC_STATIC & access) != 0) ? 0 : 1;
    final Type[] args = Type.getArgumentTypes(desc);
    for (int i = 0; i < args.length; i++) {
      variablesCount += args[i].getSize();
    }
    final int varCount = variablesCount;
    return new MethodAdapter(mv) {
      private Label myStartLabel;
      private Label myEndLabel;

      public void visitLabel(Label label) {
        if (myStartLabel == null) {
          myStartLabel = label;
        }
        myEndLabel = label;
        super.visitLabel(label);
      }

      public void visitLineNumber(final int line, final Label start) {
        getOrCreateLineData(line, name, desc);
        mv.visitVarInsn(Opcodes.ALOAD, getCurrentClassDataNumber());
        mv.visitIntInsn(Opcodes.SIPUSH, line);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "touchLine", "(" + OBJECT_TYPE + "I)V");
        if (mySize < line) mySize = line;
        super.visitLineNumber(line, start);
      }

      public void visitCode() {
        mv.visitLdcInsn(myClassData.getName());
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "loadClassData", "(Ljava/lang/String;)" + OBJECT_TYPE);
        mv.visitVarInsn(Opcodes.ASTORE, getCurrentClassDataNumber());
        super.visitCode();
      }

      public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        super.visitLocalVariable(name, desc, signature, start, end, adjustVariable(index));
      }

      public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(adjustVariable(var), increment);
      }

      public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, adjustVariable(var));
      }

      private int adjustVariable(final int var) {
        return (var >= varCount) ? var + 1 : var;
      }

      private int getCurrentClassDataNumber() {
        return varCount;
      }

      public void visitMaxs(int maxStack, int maxLocals) {
        if (myStartLabel != null && myEndLabel != null) {
          mv.visitLocalVariable("__class__data__", OBJECT_TYPE, null, myStartLabel, myEndLabel, getCurrentClassDataNumber());
        }
        super.visitMaxs(maxStack, maxLocals);
      }
    };
  }

  protected void initLineData() {
    myClassData.initLineMask(mySize);
    myClassData.setLines(LinesUtil.calcLineArray(myMaxLineNumber, myLines));
    myLines = null;
  }
}
