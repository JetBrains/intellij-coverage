package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.util.LinesUtil;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class ClassInstrumenter extends Instrumenter {
  public ClassInstrumenter(final ProjectData projectData, ClassVisitor classVisitor) {
    super(projectData, classVisitor);
  }

  protected MethodVisitor createMethodLineEnumerator(MethodVisitor mv, String name, String desc, int access, String signature,
                                           String[] exceptions) {
    return new LineEnumerator(this, mv, access, name, desc, signature, exceptions);
  }

  protected void initLineData() {
    myClassData.setLines(LinesUtil.calcLineArray(myMaxLineNumber, myLines));
    myLines = null;
  }

  public LineData getLineData(int line) {
    return (LineData) myLines.get(line);
  }

  public String getClassName() {
    return myClassData.getName();
  }
}
