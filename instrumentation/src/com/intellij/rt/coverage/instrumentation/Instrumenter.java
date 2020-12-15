/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.filters.signature.MethodSignatureFilter;
import com.intellij.rt.coverage.instrumentation.filters.visiting.MethodVisitingFilter;
import com.intellij.rt.coverage.instrumentation.kotlin.KotlinUtils;
import com.intellij.rt.coverage.util.StringsPool;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

import java.util.*;

public abstract class Instrumenter extends ClassVisitor {
  private static final List<MethodSignatureFilter> ourSignatureFilters = getMethodSignatureFilters();

  protected final ProjectData myProjectData;
  protected final ClassVisitor myClassVisitor;
  private final String myClassName;
  private final boolean myShouldCalculateSource;

  protected TIntObjectHashMap<LineData> myLines = new TIntObjectHashMap<LineData>(4, 0.99f);
  protected int myMaxLineNumber;

  protected ClassData myClassData;
  protected boolean myProcess;
  private boolean myEnum;
  private boolean myHasInterfaces = false;
  private final List<String> myAnnotations = new ArrayList<String>();
  private HashMap<String, Object> myProperties;

  public Instrumenter(final ProjectData projectData, ClassVisitor classVisitor, String className, boolean shouldCalculateSource) {
    super(Opcodes.API_VERSION, classVisitor);
    myProjectData = projectData;
    myClassVisitor = classVisitor;
    myClassName = className;
    myShouldCalculateSource = shouldCalculateSource;
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    myEnum = (access & Opcodes.ACC_ENUM) != 0;
    myProcess = (access & Opcodes.ACC_INTERFACE) == 0;
    myClassData = myProjectData.getOrCreateClassData(StringsPool.getFromPool(myClassName));
    myHasInterfaces = interfaces != null && interfaces.length > 0;
    super.visit(version, access, name, signature, superName, interfaces);
  }


  public MethodVisitor visitMethod(final int access,
                                   final String name,
                                   final String desc,
                                   final String signature,
                                   final String[] exceptions) {
    final MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    if (mv == null) return null;
    if ((access & Opcodes.ACC_BRIDGE) != 0) return mv; //try to skip bridge methods
    if ((access & Opcodes.ACC_ABSTRACT) != 0) return mv; //skip abstracts; do not include interfaces without non-abstract methods in result
    if (myEnum && isDefaultEnumMethod(name, desc, signature, myClassName)) {
      return mv;
    }
    for (MethodSignatureFilter filter : ourSignatureFilters) {
      if (filter.shouldFilter(access, name, desc, signature, exceptions, this)) {
        return mv;
      }
    }
    myProcess = true;
    return chainFilters(mv, access, name, desc, signature, exceptions);
  }

  private MethodVisitor chainFilters(MethodVisitor root, int access, String name,
                                     String desc, String signature, String[] exceptions) {
    root = createMethodLineEnumerator(root, name, desc, access, signature, exceptions);
    for (MethodVisitingFilter filter : createVisitingFilters()) {
      if (filter.isApplicable(this, access, name, desc, signature, exceptions)) {
        filter.initFilter(root, this);
        root = filter;
      }
    }
    return root;
  }

  private static boolean isDefaultEnumMethod(String name, String desc, String signature, String className) {
    return name.equals("values") && desc.equals("()[L" + className + ";") ||
           name.equals("valueOf") && desc.equals("(Ljava/lang/String;)L" + className + ";") ||
           name.equals("<init>") && signature != null && signature.equals("()V");
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
    if (myLines == null) myLines = new TIntObjectHashMap<LineData>();
    LineData lineData = myLines.get(line);
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
    if (myShouldCalculateSource) {
      myProjectData.getOrCreateClassData(myClassName).setSource(source);
    }
    if (debug != null) {
      myProjectData.addLineMaps(myClassName, JSR45Util.extractLineMapping(debug, myClassName));
    }
  }

  public String getClassName() {
    return myClassName;
  }

  public void visitOuterClass(String outerClassName, String methodName, String methodSig) {
    if (myShouldCalculateSource) {
      myProjectData.getOrCreateClassData(outerClassName).setSource(myClassData.getSource());
    }
    super.visitOuterClass(outerClassName, methodName, methodSig);
  }

  @Override
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    myAnnotations.add(StringsPool.getFromPool(descriptor));
    return super.visitAnnotation(descriptor, visible);
  }

  public boolean isSampling() {
    return myProjectData.isSampling();
  }

  public boolean hasInterfaces() {
    return myHasInterfaces;
  }

  public List<String> getAnnotations() {
    return myAnnotations;
  }

  public LineData getLineData(int line) {
    return myLines.get(line);
  }

  public Object getProperty(String key) {
    createProperties();
    return myProperties.get(key);
  }

  public void addProperty(String key, Object value) {
    createProperties();
    myProperties.put(key, value);
  }

  private void createProperties() {
    if (myProperties == null) {
      myProperties = new HashMap<String, Object>();
    }
  }

  private static List<MethodVisitingFilter> createVisitingFilters() {
    return KotlinUtils.createVisitingFilters();
  }

  private static List<MethodSignatureFilter> getMethodSignatureFilters() {
    return KotlinUtils.createSignatureFilters();
  }
}