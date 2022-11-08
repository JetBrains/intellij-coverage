/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation.dataAccess;

import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;

public abstract class CoverageDataAccess {
  /**
   * This method should access coverage data and store it to a local variable.
   */
  public abstract void onMethodStart(MethodVisitor mv, int localVariable);

  /**
   * This method is called in the end of class visiting.
   * An implementation may add extra members if needed.
   */
  public void onClassEnd(ClassVisitor cv) {
  }

  /**
   * An implementation may change method visitor to ensure correctness of coverage data.
   */
  public MethodVisitor createMethodVisitor(MethodVisitor mv, String name, boolean hasLines) {
    return mv;
  }
}
