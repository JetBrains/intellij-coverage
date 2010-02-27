package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.util.StringsPool;
import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import gnu.trove.TIntObjectHashMap;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public abstract class Instrumenter extends ClassAdapter {
  protected final ProjectData myProjectData;
  protected final ClassVisitor myClassVisitor;

  protected TIntObjectHashMap myLines = new TIntObjectHashMap(4, 0.99f);
  protected int myMaxLineNumber;

  protected ClassData myClassData;
  protected boolean myProcess;
  private boolean myEnum;

  public Instrumenter(final ProjectData projectData, ClassVisitor classVisitor) {
    super(classVisitor);
    myProjectData = projectData;
    myClassVisitor = classVisitor;
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    final String className = name.replace('/', '.');
    if ((access & Opcodes.ACC_INTERFACE) == 0) {
      myProcess = true;
      myEnum = (access & Opcodes.ACC_ENUM) != 0;
      myClassData = myProjectData.getOrCreateClassData(StringsPool.getFromPool(className));
    }
    super.visit(version, access, name, signature, superName, interfaces);
  }


  public MethodVisitor visitMethod(final int access,
                                   final String name,
                                   final String desc,
                                   final String signature,
                                   final String[] exceptions) {
    final MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    if (!myProcess || mv == null) return mv;
    if (myEnum) {
      final String className = myClassData.getName().replace('.', '/');
      if (name.equals("values") && desc.equals("()[L" + className + ";")) return mv;
      if (name.equals("valueOf") && desc.equals("(Ljava/lang/String;)L" + className + ";")) return mv;
      if (name.equals("<init>") && signature != null && signature.equals("()V")) return mv;
    }
    return createMethodLineEnumerator(mv, name, desc, access, signature, exceptions);
  }

  protected abstract MethodVisitor createMethodLineEnumerator(MethodVisitor mv, String name, String desc, int access, String signature, String[] exceptions);

  public void visitEnd() {
    if (myProcess) {
      initLineData();
    }
    super.visitEnd();
  }

  protected abstract void initLineData();

  protected void getOrCreateLineData(int line, String name, String desc) {
    //create lines again if class was loaded again by another class loader; may be myLinesArray should be cleared
    if (myLines == null) myLines = new TIntObjectHashMap();
    LineData lineData = (LineData) myLines.get(line);
    if (lineData == null) {
      lineData = new LineData(line, StringsPool.getFromPool(name + desc));
      myLines.put(line, lineData);
    }
    if (line > myMaxLineNumber) myMaxLineNumber = line;
  }

  public void removeLine(final int line) {
    myLines.remove(line);
  }

  public int getMaxLineNumber() {
    return myMaxLineNumber;
  }
}