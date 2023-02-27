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

package com.intellij.rt.coverage.instrumentation.filters.classFilter;

import com.intellij.rt.coverage.instrumentation.Instrumenter;
import com.intellij.rt.coverage.util.ClassNameUtil;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;

/**
 * Deprecated/ignored methods may contain anonymous classes, which should also be ignored.
 * A class has OUTER CLASS property, that references the declaration method.
 *
 * @see com.intellij.rt.coverage.instrumentation.filters.lines.KotlinDeprecatedMethodFilter
 * @see com.intellij.rt.coverage.instrumentation.filters.lines.AnnotationIgnoredMethodFilter
 */
public class KotlinAnonymousClassInIgnoredMethodFilter extends ClassFilter {
  private boolean myShouldIgnoreClass = false;

  @Override
  public boolean isApplicable(Instrumenter context) {
    return true;
  }

  @Override
  public void visitOuterClass(String owner, String name, String descriptor) {
    super.visitOuterClass(owner, name, descriptor);
    if (myShouldIgnoreClass) return;
    String ownerName = ClassNameUtil.convertToFQName(owner);
    boolean ignored = myContext.getProjectData().isMethodIgnored(ownerName, name, descriptor);
    if (ignored) {
      myContext.setIgnoreSection(true);
      myShouldIgnoreClass = true;
    }
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
    if (myShouldIgnoreClass) {
      myContext.getProjectData().addIgnoredMethod(myContext.getClassName(), name, descriptor);
    }
    return super.visitMethod(access, name, descriptor, signature, exceptions);
  }

  @Override
  public void visitEnd() {
    if (myShouldIgnoreClass) {
      myContext.setIgnoreSection(false);
    }
    super.visitEnd();
  }
}
