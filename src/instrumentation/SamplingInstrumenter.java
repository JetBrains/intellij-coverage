package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ProjectData;
import org.objectweb.asm.*;

public class SamplingInstrumenter extends Instrumenter {
  private int mySize;

  public SamplingInstrumenter(final ProjectData projectData, ClassVisitor classVisitor) {
    super(projectData, classVisitor);
  }

  protected MethodVisitor createMethodLineEnumerator(final MethodVisitor mv,
                                                     final String name,
                                                     final String desc,
                                                     final int access,
                                                     final String signature,
                                                     final String[] exceptions) {
    return new MethodAdapter(mv) {
      public void visitLineNumber(final int line, final Label start) {
        myClassData.getOrCreateLine(line, name + desc);
        mv.visitLdcInsn(myClassData.getName());
        mv.visitIntInsn(Opcodes.SIPUSH, line);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "touchLine", "(Ljava/lang/String;I)V");
        if (mySize < line) mySize = line;
        super.visitLineNumber(line, start);
      }
    };
  }

  protected void initLineData() {
    myClassData.initLineMask(mySize);
  }
}
