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
import com.intellij.rt.coverage.util.OptionsUtil;
import org.jetbrains.coverage.org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;

public class InstrumentationVisitor extends ClassVisitor {
  private static final List<MethodFilter> ourMethodFilters = FilterUtils.createMethodFilters();

  private final InstrumentationData myContext;
  private final CoverageDataAccessVisitor myDataAccess;
  private final ProjectData myProjectData;
  private String mySource;


  public InstrumentationVisitor(ProjectData projectData,
                                final InstrumentationData context,
                                ClassVisitor classVisitor,
                                CoverageDataAccess dataAccess) {
    super(Opcodes.API_VERSION, new CoverageDataAccessVisitor(classVisitor, dataAccess) {
      @Override
      protected boolean shouldInstrumentMethod() {
        return !context.hasNoLinesInCurrentMethod();
      }
    });
    myDataAccess = (CoverageDataAccessVisitor) cv;
    myContext = context;
    myProjectData = projectData;
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    myContext.put(Key.CLASS_ACCESS, access);
    myContext.put(Key.INTERFACES, interfaces);
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    List<String> annotations = myContext.get(Key.CLASS_ANNOTATIONS);
    if (annotations == null) {
      annotations = new ArrayList<String>();
      myContext.put(Key.CLASS_ANNOTATIONS, annotations);
    }
    annotations.add(myContext.getProjectContext().getFromPool(descriptor));
    return super.visitAnnotation(descriptor, visible);
  }


  @Override
  public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
    myContext.resetMethod();

    myContext.put(Key.METHOD_ACCESS, access);
    myContext.put(Key.METHOD_NAME, name);
    myContext.put(Key.METHOD_DESC, descriptor);
    myContext.put(Key.METHOD_SIGNATURE, signature);
    myContext.put(Key.EXCEPTIONS, exceptions);

    if (!shouldInstrumentMethod(access)) return super.visitMethod(access, name, descriptor, signature, exceptions);

    boolean branchCoverage = getOptions().isBranchCoverage;
    final CoverageEnumerator enumerator = getOptions().isInstructionCoverage
        ? new CoverageEnumeratorWithInstructions(myContext, branchCoverage)
        : new CoverageEnumerator(myContext, branchCoverage);
    MethodVisitor mv = chainFilters(enumerator, FilterUtils.createLineFilters());
    if (branchCoverage) {
      mv = chainFilters(mv, FilterUtils.createBranchFilters());
    }

    return new MethodVisitor(Opcodes.API_VERSION, mv) {
      @Override
      public void visitEnd() {
        super.visitEnd();
        if (myDataAccess.getDataAccess() != EmptyCoverageDataAccess.INSTANCE) {
          MethodVisitor methodVisitor = InstrumentationVisitor.super.visitMethod(access, name, descriptor, signature, exceptions);
          enumerator.accept(myContext.hasNoLinesInCurrentMethod() ? methodVisitor : new HitsVisitor(methodVisitor));
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
      if (filter.shouldFilter(myContext)) {
        return false;
      }
    }
    return true;
  }

  private MethodVisitor chainFilters(MethodVisitor root, List<CoverageFilter> filters) {
    for (CoverageFilter filter : filters) {
      if (filter.isApplicable(myContext)) {
        filter.initFilter(root, myContext);
        root = filter;
      }
    }
    return root;
  }

  @Override
  public void visitEnd() {
    super.visitEnd();
    String className = myContext.get(Key.CLASS_NAME);
    ClassData classData = myProjectData.getOrCreateClassData(className);
    classData.setLines(LinesUtil.calcLineArray(myContext.getMaxSeenLine(), myContext.getLines()));
    classData.createMask(myContext.getSize(), getOptions().isCalculateHits);
    classData.setSource(mySource);
    myContext.getProjectContext().addIgnoredLines(className, myContext.getIgnoredLines());
    if (OptionsUtil.TEST_MODE) {
      InstrumentationData.assertIds(classData);
    }
    if (getOptions().isInstructionCoverage) {
      ClassInstructions classInstructions = new ClassInstructions(classData, myContext.getInstructions());
      myProjectData.getInstructions().put(classData.getName(), classInstructions);
    }
  }

  @Override
  public void visitSource(String source, String debug) {
    super.visitSource(source, debug);
    String className = myContext.get(Key.CLASS_NAME);
    if (getOptions().isSaveSource) {
      mySource = myContext.getProjectContext().getFromPool(source);
    }
    if (debug != null) {
      FileMapData[] mapping = JSR45Util.extractLineMapping(debug, className);
      if (mapping != null) {
        myContext.getProjectContext().addLineMaps(className, mapping);
      }
    }
  }

  @Override
  public void visitOuterClass(String outerClassName, String methodName, String methodSig) {
    if (getOptions().isSaveSource) {
      String fqnName = ClassNameUtil.convertToFQName(outerClassName);
      ClassData outerClass = myProjectData.getOrCreateClassData(myContext.getProjectContext().getFromPool(fqnName));
      if (outerClass.getSource() == null) {
        outerClass.setSource(mySource);
      }
    }
    super.visitOuterClass(outerClassName, methodName, methodSig);
  }

  private InstrumentationOptions getOptions() {
    return myContext.getProjectContext().getOptions();
  }

  private class HitsVisitor extends MethodVisitor {

    public HitsVisitor(MethodVisitor methodVisitor) {
      super(Opcodes.API_VERSION, methodVisitor);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
      LineData lineData = myContext.getLineData(line);
      if (lineData != null) {
        incrementHitById(lineData.getId());
      }
      super.visitLineNumber(line, start);
    }

    @Override
    public void visitLabel(Label label) {
      super.visitLabel(label);

      Jump jump = myContext.getJump(label);
      if (jump != null) {
        incrementHitById(jump.getId());
      }

      Switch aSwitch = myContext.getSwitch(label);
      if (aSwitch != null) {
        incrementHitById(aSwitch.getId());
      }
    }

    private void incrementHitById(int id) {
      if (id == -1) return;
      myDataAccess.loadFromLocal();
      InstrumentationUtils.touchById(mv, id, getOptions().isCalculateHits);
    }
  }
}
