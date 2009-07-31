package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.ProjectData;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public abstract class Instrumenter extends ClassAdapter {
  protected final ProjectData myProjectData;
  protected final ClassVisitor myClassVisitor;

  protected ClassData myClassData;
  protected boolean myProcess;
  private boolean myEnum;
  protected boolean myHasStaticInitializer;

  public Instrumenter(final ProjectData projectData, ClassVisitor classVisitor) {
    super(classVisitor);
    myProjectData = projectData;
    myClassVisitor = classVisitor;
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    if (name.startsWith("com/intellij/rt/coverage")) { //do not instrument itself
      super.visit(version, access, name, signature, superName, interfaces);
      return;
    }
    final String className = name.replace('/', '.');
    if ((access & Opcodes.ACC_INTERFACE) == 0) {
      myProcess = true;
      myEnum = (access & Opcodes.ACC_ENUM) != 0;
      myClassData = myProjectData.getOrCreateClassData(className);

      myClassVisitor.visitField(Opcodes.ACC_STATIC, "__class__data__", ClassData.CLASS_DATA_TYPE, null, null).visitEnd();
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
    if (name.equals("<clinit>") && (access & Opcodes.ACC_STATIC) != 0){
      myHasStaticInitializer = true;
      return createMethodLineEnumerator(mv, true, name, desc, access, signature, exceptions);
    }
    return createMethodLineEnumerator(mv, false, name, desc, access, signature, exceptions);
  }

  protected abstract MethodVisitor createMethodLineEnumerator(MethodVisitor mv, boolean staticInitializer, String name, String desc, int access, String signature, String[] exceptions);

  protected void createClassDataField(final MethodVisitor mv) {
    createClassDataField(mv, myClassData.getName());
  }

   protected static void createClassDataField(final MethodVisitor mv, final String ownerName) {
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, ProjectData.PROJECT_DATA_OWNER, "getProjectData", "()" + ProjectData.PROJECT_DATA_TYPE);
    mv.visitLdcInsn(ownerName);
    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ProjectData.PROJECT_DATA_OWNER, "getClassData", "(Ljava/lang/String;)" + ClassData.CLASS_DATA_TYPE);
    mv.visitFieldInsn(Opcodes.PUTSTATIC, ownerName.replace('.', '/'), "__class__data__", ClassData.CLASS_DATA_TYPE);
  }

   public void visitEnd() {
    if (myProcess) {
      if (!myHasStaticInitializer) {
        final MethodVisitor mv = myClassVisitor.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        createClassDataField(mv);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
      }
      initLineData();
    }
    super.visitEnd();
  }

  protected abstract void initLineData();
}