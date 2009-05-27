package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ProjectData;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class ClassInstrumenter extends Instrumenter {
  public ClassInstrumenter(final ProjectData projectData, ClassVisitor classVisitor) {
    super(projectData, classVisitor);
  }

  protected MethodVisitor createMethodLineEnumerator(MethodVisitor mv, boolean staticInitializer, String name, String desc, int access, String signature,
                                           String[] exceptions) {
    return new LineEnumerator(myClassData, staticInitializer, mv, access, name, desc, signature, exceptions);
  }

  protected void initLineData() {
    myClassData.fillArray();
  }
}
