package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.util.StringsPool;
import gnu.trove.TIntObjectHashMap;
import org.jetbrains.asm4.ClassVisitor;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Opcodes;

public abstract class Instrumenter extends ClassVisitor {
  protected final ProjectData myProjectData;
  protected final ClassVisitor myClassVisitor;
  private final String myClassName;

  protected TIntObjectHashMap myLines = new TIntObjectHashMap(4, 0.99f);
  protected int myMaxLineNumber;

  protected ClassData myClassData;
  protected boolean myProcess;
  private boolean myEnum;

  public Instrumenter(final ProjectData projectData, ClassVisitor classVisitor, String className) {
    super(Opcodes.ASM4, classVisitor);
    myProjectData = projectData;
    myClassVisitor = classVisitor;
    myClassName = className;
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    if ((access & Opcodes.ACC_INTERFACE) == 0) {
      myProcess = true;
      myEnum = (access & Opcodes.ACC_ENUM) != 0;
      myClassData = myProjectData.getOrCreateClassData(StringsPool.getFromPool(myClassName));
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
    if ((access & Opcodes.ACC_BRIDGE) != 0) return mv; //try to skip bridge methods
    if (myEnum) {
      if (name.equals("values") && desc.equals("()[L" + myClassName + ";")) return mv;
      if (name.equals("valueOf") && desc.equals("(Ljava/lang/String;)L" + myClassName + ";")) return mv;
      if (name.equals("<init>") && signature != null && signature.equals("()V")) return mv;
    }
    return createMethodLineEnumerator(mv, name, desc, access, signature, exceptions);
  }

  protected abstract MethodVisitor createMethodLineEnumerator(MethodVisitor mv, String name, String desc, int access, String signature, String[] exceptions);

  public void visitEnd() {
    if (myProcess) {
      initLineData();
      myLines = null;
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

  public void visitSource(String source, String debug) {
    super.visitSource(source, debug);
    if (debug != null) {
      myProjectData.addLineMaps(myClassName, JSR45Util.extractLineMapping(debug, myClassName));
    }
  }

  public String getClassName() {
    return myClassName;
  }
}