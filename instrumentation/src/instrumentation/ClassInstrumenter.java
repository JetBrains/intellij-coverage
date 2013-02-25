package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.util.LinesUtil;
import org.jetbrains.asm4.ClassVisitor;
import org.jetbrains.asm4.MethodVisitor;

public class ClassInstrumenter extends Instrumenter {
  public ClassInstrumenter(final ProjectData projectData, ClassVisitor classVisitor, String className, boolean shouldCalculateSource) {
    super(projectData, classVisitor, className, shouldCalculateSource);
  }

  protected MethodVisitor createMethodLineEnumerator(MethodVisitor mv, String name, String desc, int access, String signature,
                                           String[] exceptions) {
    return new LineEnumerator(this, mv, access, name, desc, signature, exceptions);
  }

  protected void initLineData() {
    myClassData.setLines(LinesUtil.calcLineArray(myMaxLineNumber, myLines));
  }

  public LineData getLineData(int line) {
    return (LineData) myLines.get(line);
  }

}
