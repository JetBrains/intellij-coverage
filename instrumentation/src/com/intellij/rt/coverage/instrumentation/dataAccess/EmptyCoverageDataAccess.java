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

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.UnloadedUtil;
import com.intellij.rt.coverage.instrumentation.data.ProjectContext;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;

/**
 * This data access may be used for class analysis without actual transform and further usage.
 *
 * @see UnloadedUtil#appendUnloaded(ProjectData, ProjectContext)
 */
public class EmptyCoverageDataAccess extends CoverageDataAccess {
  public static final EmptyCoverageDataAccess INSTANCE = new EmptyCoverageDataAccess();

  public EmptyCoverageDataAccess() {
    super(null);
  }

  @Override
  public void onMethodStart(MethodVisitor mv, int localVariable) {
  }
}
