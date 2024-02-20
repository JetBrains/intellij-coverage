/*
 * Copyright 2000-2024 JetBrains s.r.o.
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

import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.data.Key;
import com.intellij.rt.coverage.util.ClassNameUtil;

/**
 * This is a utility filter that collects the required information that is later used in reporter.
 */
public class InheritanceCollectorFilter extends ClassFilter {
  private static final String[] EMPTY = new String[0];
  private static final String[] OBJECT = new String[]{"java.lang.Object"};

  @Override
  public boolean isApplicable(InstrumentationData context) {
    return context.getProjectContext().shouldCollectInherits();
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    int size = (superName == null ? 0 : 1) + (interfaces == null ? 0 : interfaces.length);
    boolean hasNoSuper = (interfaces == null || interfaces.length == 0) && "java/lang/Object".equals(superName);
    String[] inherits;
    if (size == 0) {
      inherits = EMPTY;
    } else if (hasNoSuper) {
      inherits = OBJECT;
    } else {
      inherits = new String[size];
      if (interfaces != null) {
        System.arraycopy(interfaces, 0, inherits, 0, interfaces.length);
      }
      if (superName != null) {
        inherits[size - 1] = superName;
      }

      for (int i = 0; i < size; i++) {
        inherits[i] = myContext.getProjectContext().getFromPool(ClassNameUtil.convertToFQName(inherits[i]));
      }
    }

    myContext.getProjectContext().addInherits(myContext.get(Key.CLASS_NAME), inherits);
  }
}
