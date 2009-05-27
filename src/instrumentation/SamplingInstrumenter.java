package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.ProjectData;
import org.objectweb.asm.*;

public class SamplingInstrumenter extends Instrumenter {
  private int mySize;

  public SamplingInstrumenter(final ProjectData projectData, ClassVisitor classVisitor) {
    super(projectData, classVisitor);
  }

  protected MethodVisitor createMethodLineEnumerator(final MethodVisitor mv,
                                                     final boolean staticInitializer,
                                                     final String name,
                                                     final String desc,
                                                     final int access,
                                                     final String signature,
                                                     final String[] exceptions) {
    return new MethodAdapter(mv) {
      public void visitLineNumber(final int line, final Label start) {
        myClassData.addLine(line, name + desc);
        mv.visitFieldInsn(Opcodes.GETSTATIC, myClassData.getName().replace('.', '/'), "__class__data__", ClassData.CLASS_DATA_TYPE);
        mv.visitIntInsn(Opcodes.SIPUSH, line);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ClassData.CLASS_DATA_OWNER, "touchLine", "(I)V");

        if (mySize < line) mySize = line;
        super.visitLineNumber(line, start);
      }

      public void visitCode() {
        if (staticInitializer) {
          createClassDataField(mv);
        }
        super.visitCode();
      }
    };
  }

  protected void initLineData() {
    myClassData.initLineMask(mySize);
  }
}
