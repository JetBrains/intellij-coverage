/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.filters.enumerating.LineEnumeratorFilter;
import com.intellij.rt.coverage.instrumentation.kotlin.KotlinUtils;
import com.intellij.rt.coverage.util.LinesUtil;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;

import java.util.List;

public class ClassInstrumenter extends Instrumenter {
  public ClassInstrumenter(final ProjectData projectData, ClassVisitor classVisitor, String className, boolean shouldCalculateSource) {
    super(projectData, classVisitor, className, shouldCalculateSource);
  }

  protected MethodVisitor createMethodLineEnumerator(MethodVisitor mv, String name, String desc, int access, String signature,
                                                     String[] exceptions) {
    final LineEnumerator enumerator = new LineEnumerator(this, mv, access, name, desc, signature, exceptions);
    return chainFilters(name, desc, access, signature, exceptions, enumerator);
  }

  private MethodVisitor chainFilters(String name, String desc, int access, String signature, String[] exceptions,
                                     LineEnumerator enumerator) {
    MethodVisitor root = enumerator;
    for (LineEnumeratorFilter filter : createLineEnumeratorFilters()) {
      if (filter.isApplicable(this, access, name, desc, signature, exceptions)) {
        filter.initFilter(root, enumerator);
        root = filter;
      }
    }
    return root;
  }

  protected void initLineData() {
    myClassData.setLines(LinesUtil.calcLineArray(myMaxLineNumber, myLines));
  }

  private static List<LineEnumeratorFilter> createLineEnumeratorFilters() {
    return KotlinUtils.createLineEnumeratorFilters();
  }
}
