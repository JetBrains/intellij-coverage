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

import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

/**
 * Filter ignores synthetic methods and thus no information e.g. about lambda body would be collected
 */
class InstrumentedMethodsFilter {
  private final String myClassName;
  private boolean myEnum;

  InstrumentedMethodsFilter(String className) {
    myClassName = className;
  }

  void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    myEnum = (access & Opcodes.ACC_ENUM) != 0;
  }

  Decision shouldVisitMethod(final int access,
                             final String name,
                             final String desc,
                             final String signature,
                             final String[] exceptions,
                             boolean instrumentConstructorsForce) {
    if ((access & Opcodes.ACC_BRIDGE) != 0) return Decision.NO; //try to skip bridge methods
    if ((access & Opcodes.ACC_ABSTRACT) != 0) return Decision.NO; //skip abstracts; do not include interfaces without non-abstract methods in result
    if ("<clinit>".equals(name) || //static initializer
        (access & Opcodes.ACC_SYNTHETIC) != 0) {//skip all synthetic methods
      return Decision.NO;
    }
    if (name.equals("<init>") && desc.equals("()V")) {
      // should check deep
      return instrumentConstructorsForce ? Decision.YES : Decision.CHECK_IS_CONSTRUCTOR_DEFAULT;
    }

    if (myEnum && isDefaultEnumMethod(name, desc, signature, myClassName)) {
      return Decision.NO;
    }
    return Decision.YES;
  }

  private static boolean isDefaultEnumMethod(String name, String desc, String signature, String className) {
    return name.equals("values") && desc.equals("()[L" + className + ";") ||
        name.equals("valueOf") && desc.equals("(Ljava/lang/String;)L" + className + ";") ||
        name.equals("<init>") && signature != null && signature.equals("()V");
  }

  enum Decision {
      YES, NO, CHECK_IS_CONSTRUCTOR_DEFAULT
  }
}
