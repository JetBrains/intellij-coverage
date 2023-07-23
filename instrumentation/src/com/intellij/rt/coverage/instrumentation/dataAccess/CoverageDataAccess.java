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

import com.intellij.rt.coverage.instrumentation.InstrumentationUtils;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;

/**
 * This class encapsulates access to coverage data in runtime,
 * so that different strategies could be implemented.
 */
public abstract class CoverageDataAccess {
  protected final Init myInit;

  public CoverageDataAccess(Init init) {
    myInit = init;
  }

  public Init getInit() {
    return myInit;
  }

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

  /**
   * The init information about a method which should be invoked to get coverage data
   * with its signature and parameter values.
   */
  public static class Init {
    public final String name;
    public final String desc;
    public final String initOwner;
    public final String initName;
    public final String initDesc;
    public final Object[] params;

    public Init(String name, String desc, String initOwner, String initName, String initDesc, Object[] params) {
      this.name = name;
      this.desc = desc;
      this.initOwner = initOwner;
      this.initName = initName;
      this.initDesc = initDesc;
      this.params = params;
    }

    public void loadParams(MethodVisitor mv) {
      for (Object param : params) {
        InstrumentationUtils.push(mv, param);
      }
    }
  }
}
