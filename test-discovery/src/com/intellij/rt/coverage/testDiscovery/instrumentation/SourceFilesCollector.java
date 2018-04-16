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

package com.intellij.rt.coverage.testDiscovery.instrumentation;

import com.intellij.rt.coverage.instrumentation.JSR45Util;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;

import java.util.ArrayList;
import java.util.List;

class SourceFilesCollector extends ClassVisitor {
  private final List<String> sources = new ArrayList<String>(1);
  private final String className;

  SourceFilesCollector(int api, ClassVisitor cv, String className) {
    super(api, cv);
    this.className = className;
  }

  public List<String> getSources() {
    return sources;
  }

  @Override
  public void visitSource(String source, String debug) {
    if (debug != null) {
      // SourceDebugExtension attribute
      sources.addAll(JSR45Util.parseSourcePaths(debug));
    }
    else {
      // SourceFile attribute
      sources.add(JSR45Util.getClassPackageName(className).replace(".", "/") + source);
    }
    super.visitSource(source, debug);
  }
}
