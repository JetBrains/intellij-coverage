/*
 * User: anna
 * Date: 27-Jun-2008
 */
package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ClassData;
import gnu.trove.TIntObjectHashMap;
import org.objectweb.asm.*;

import java.util.HashSet;
import java.util.Set;

public class SourceLineCounter extends ClassVisitor {
  private final boolean myExcludeLines;
  private final ClassData myClassData;

  private final TIntObjectHashMap myNSourceLines = new TIntObjectHashMap();
  private final Set myMethodsWithSourceCode = new HashSet();
  private int myCurrentLine;
  private boolean myInterface;
  private boolean myEnum;

  public SourceLineCounter(final ClassData classData, final boolean excludeLines) {
    super(Opcodes.ASM4, new ClassVisitor(Opcodes.ASM4) {});
    myClassData = classData;
    myExcludeLines = excludeLines;
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    myInterface = (access & Opcodes.ACC_INTERFACE) != 0;
    myEnum = (access & Opcodes.ACC_ENUM) != 0;
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public MethodVisitor visitMethod(final int access,
                                   final String name,
                                   final String desc,
                                   final String signature,
                                   final String[] exceptions) {
    final MethodVisitor v = cv.visitMethod(access, name, desc, signature, exceptions);
    if (myInterface) return v;
    if (myEnum) {
      if (name.equals("values") && desc.startsWith("()[L")) return v;
      if (name.equals("valueOf") && desc.startsWith("(Ljava/lang/String;)L")) return v;
      if (name.equals("<init>") && signature != null && signature.equals("()V")) return v;
    }
    return new MethodVisitor(Opcodes.ASM4, v) {
      private boolean myHasInstructions;


      public void visitLineNumber(final int line, final Label start) {
        myHasInstructions = false;
        myCurrentLine = line;
        if (!myExcludeLines ||
            myClassData == null ||
            myClassData.getStatus(name + desc) != null ||
            (!name.equals("<init>") && !name.equals("<clinit>"))) {
          myNSourceLines.put(line, name + desc);
          myMethodsWithSourceCode.add(name + desc);
        }
      }

      public void visitInsn(final int opcode) {
        if (myExcludeLines) {
          if (opcode == Opcodes.RETURN && !myHasInstructions) {
            myNSourceLines.remove(myCurrentLine);
          } else {
            myHasInstructions = true;
          }
        }
      }

      public void visitIntInsn(final int opcode, final int operand) {
        super.visitIntInsn(opcode, operand);
        myHasInstructions = true;
      }

      public void visitVarInsn(final int opcode, final int var) {
        super.visitVarInsn(opcode, var);
        myHasInstructions = true;
      }

      public void visitTypeInsn(final int opcode, final String type) {
        super.visitTypeInsn(opcode, type);
        myHasInstructions = true;
      }

      public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
        super.visitFieldInsn(opcode, owner, name, desc);
        myHasInstructions = true;
      }

      public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc) {
        super.visitMethodInsn(opcode, owner, name, desc);
        myHasInstructions = true;
      }

      public void visitJumpInsn(final int opcode, final Label label) {
        super.visitJumpInsn(opcode, label);
        myHasInstructions = true;
      }

      public void visitLdcInsn(final Object cst) {
        super.visitLdcInsn(cst);
        myHasInstructions = true;
      }

     
      public void visitIincInsn(final int var, final int increment) {
        super.visitIincInsn(var, increment);
        myHasInstructions = true;
      }

     
      public void visitTableSwitchInsn(final int min, final int max, final Label dflt, final Label[] labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        myHasInstructions = true;
      }

     
      public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        myHasInstructions = true;
      }

     
      public void visitMultiANewArrayInsn(final String desc, final int dims) {
        super.visitMultiANewArrayInsn(desc, dims);
        myHasInstructions = true;
      }

     
      public void visitTryCatchBlock(final Label start, final Label end, final Label handler, final String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        myHasInstructions = true;
      }

    };
  }

  public int getNSourceLines() {
    return myNSourceLines.size();
  }

  public TIntObjectHashMap getSourceLines() {
    return myNSourceLines;
  }


  public Set getMethodsWithSourceCode() {
    return myMethodsWithSourceCode;
  }

  public int getNMethodsWithCode() {
    return myMethodsWithSourceCode.size();
  }

  public boolean isInterface() {
    return myInterface;
  }
}
