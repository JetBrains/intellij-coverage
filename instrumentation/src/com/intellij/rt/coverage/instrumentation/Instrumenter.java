/*
 * Copyright 2000-2023 JetBrains s.r.o.
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
import com.intellij.rt.coverage.data.FileMapData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.data.instructions.ClassInstructions;
import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.data.Jump;
import com.intellij.rt.coverage.instrumentation.data.Key;
import com.intellij.rt.coverage.instrumentation.data.Switch;
import com.intellij.rt.coverage.instrumentation.dataAccess.CoverageDataAccess;
import com.intellij.rt.coverage.instrumentation.dataAccess.CoverageDataAccessVisitor;
import com.intellij.rt.coverage.instrumentation.dataAccess.EmptyCoverageDataAccess;
import com.intellij.rt.coverage.instrumentation.filters.FilterUtils;
import com.intellij.rt.coverage.instrumentation.filters.lines.CoverageFilter;
import com.intellij.rt.coverage.instrumentation.filters.methods.MethodFilter;
import com.intellij.rt.coverage.instrumentation.util.LinesUtil;
import com.intellij.rt.coverage.util.ClassNameUtil;
import org.jetbrains.coverage.org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;

public class Instrumenter extends ClassVisitor {
  private static final List<MethodFilter> ourMethodFilters = FilterUtils.createMethodFilters();

  private final InstrumentationData myData;
  private final CoverageDataAccessVisitor myDataAccess;
  private final ProjectData myProjectData;
  private final boolean myBranchCoverage;
  private final boolean mySaveSource;
  private final boolean myCalculateHits;
  private String mySource;


  public Instrumenter(ClassVisitor classVisitor,
                      CoverageDataAccess dataAccess,
                      final InstrumentationData data,
                      ProjectData projectData,
                      boolean branchCoverage,
                      boolean saveSource,
                      boolean calculateHits) {
    super(Opcodes.API_VERSION, new CoverageDataAccessVisitor(classVisitor, dataAccess) {
      @Override
      protected boolean shouldInstrumentMethod() {
        return !data.hasNoLinesInCurrentMethod();
      }
    });
    myDataAccess = (CoverageDataAccessVisitor) cv;
    myData = data;
    myProjectData = projectData;
    myBranchCoverage = branchCoverage;
    myCalculateHits = calculateHits;
    mySaveSource = saveSource;
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    myData.put(Key.CLASS_ACCESS, access);
    myData.put(Key.INTERFACES, interfaces);
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    List<String> annotations = myData.get(Key.CLASS_ANNOTATIONS);
    if (annotations == null) {
      annotations = new ArrayList<String>();
      myData.put(Key.CLASS_ANNOTATIONS, annotations);
    }
    annotations.add(myProjectData.getFromPool(descriptor));
    return super.visitAnnotation(descriptor, visible);
  }


  @Override
  public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
    myData.resetMethod();

    myData.put(Key.METHOD_ACCESS, access);
    myData.put(Key.METHOD_NAME, name);
    myData.put(Key.METHOD_DESC, descriptor);
    myData.put(Key.METHOD_SIGNATURE, signature);
    myData.put(Key.EXCEPTIONS, exceptions);

    if (!shouldInstrumentMethod(access)) return super.visitMethod(access, name, descriptor, signature, exceptions);

    final CoverageEnumerator enumerator = myProjectData.isInstructionsCoverageEnabled()
        ? new CoverageEnumeratorWithInstructions(myData, myBranchCoverage)
        : new CoverageEnumerator(myData, myBranchCoverage);
    MethodVisitor mv = chainFilters(enumerator, FilterUtils.createLineFilters());
    if (myBranchCoverage) {
      mv = chainFilters(mv, FilterUtils.createBranchFilters());
    }

    return new MethodVisitor(Opcodes.API_VERSION, mv) {
      @Override
      public void visitEnd() {
        super.visitEnd();
        if (myDataAccess.getDataAccess() != EmptyCoverageDataAccess.INSTANCE) {
          MethodVisitor methodVisitor = Instrumenter.super.visitMethod(access, name, descriptor, signature, exceptions);
          enumerator.accept(myData.hasNoLinesInCurrentMethod() ? methodVisitor : new HitsVisitor(methodVisitor));
        }
      }
    };
  }

  /**
   * Should be called only after first <code>visitMethod</code> has been called.
   */
  private boolean shouldInstrumentMethod(int access) {
    //try to skip bridge methods
    if ((access & Opcodes.ACC_BRIDGE) != 0) return false;
    // skip abstracts; do not include interfaces without non-abstract methods in result
    if ((access & Opcodes.ACC_ABSTRACT) != 0) return false;

    for (MethodFilter filter : ourMethodFilters) {
      if (filter.shouldFilter(myData)) {
        return false;
      }
    }
    return true;
  }

  private MethodVisitor chainFilters(MethodVisitor root, List<CoverageFilter> filters) {
    for (CoverageFilter filter : filters) {
      if (filter.isApplicable(myData)) {
        filter.initFilter(root, myData);
        root = filter;
      }
    }
    return root;
  }

  @Override
  public void visitEnd() {
    super.visitEnd();
    ClassData classData = myProjectData.getOrCreateClassData(myData.get(Key.CLASS_NAME));
    classData.setLines(LinesUtil.calcLineArray(myData.getMaxSeenLine(), myData.getLines()));
    classData.createMask(myData.getSize(), myCalculateHits);
    classData.setSource(mySource);
    classData.setIgnoredLines(myData.getIgnoredLines());
    if (myProjectData.isInstructionsCoverageEnabled()) {
      ClassInstructions classInstructions = new ClassInstructions(classData, myData.getInstructions());
      myProjectData.getInstructions().put(classData.getName(), classInstructions);
    }
  }

  @Override
  public void visitSource(String source, String debug) {
    super.visitSource(source, debug);
    String className = myData.get(Key.CLASS_NAME);
    if (mySaveSource) {
      mySource = source;
    }
    if (debug != null) {
      FileMapData[] mapping = JSR45Util.extractLineMapping(debug, className);
      if (mapping != null) {
        myProjectData.addLineMaps(className, mapping);
      }
    }
  }

  @Override
  public void visitOuterClass(String outerClassName, String methodName, String methodSig) {
    if (mySaveSource) {
      String fqnName = ClassNameUtil.convertToFQName(outerClassName);
      ClassData outerClass = myProjectData.getOrCreateClassData(fqnName);
      if (outerClass.getSource() == null) {
        outerClass.setSource(mySource);
      }
    }
    super.visitOuterClass(outerClassName, methodName, methodSig);
  }


  private class HitsVisitor extends MethodVisitor {

    public HitsVisitor(MethodVisitor methodVisitor) {
      super(Opcodes.API_VERSION, methodVisitor);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
      LineData lineData = myData.getLineData(line);
      if (lineData != null) {
        incrementHitById(lineData.getId());
      }
      super.visitLineNumber(line, start);
    }

    @Override
    public void visitLabel(Label label) {
      super.visitLabel(label);

      Jump jump = myData.getJump(label);
      if (jump != null) {
        incrementHitById(jump.getId());
      }

      Switch aSwitch = myData.getSwitch(label);
      if (aSwitch != null) {
        incrementHitById(aSwitch.getId());
      }
    }

    private void incrementHitById(int id) {
      if (id == -1) return;
      myDataAccess.loadFromLocal();
      InstrumentationUtils.touchById(mv, id, myCalculateHits);
    }
  }
}
