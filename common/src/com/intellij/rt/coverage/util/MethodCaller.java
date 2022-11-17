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

package com.intellij.rt.coverage.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This is a wrapper for method calls via reflection.
 */
public class MethodCaller {
  private Method myMethod;
  private final String myMethodName;
  private final Class<?>[] myParamTypes;

  public MethodCaller(final String methodName, final Class<?>[] paramTypes) {
    myMethodName = methodName;
    myParamTypes = paramTypes;
  }

  public Object invoke(Object thisObj, final Object[] paramValues) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    if (myMethod == null) {
      myMethod = findMethod(thisObj.getClass(), myMethodName, myParamTypes);
    }
    return myMethod.invoke(thisObj, paramValues);
  }

  public Object invokeStatic(String className, final Object[] paramValues, ClassLoader loader) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException {
    if (myMethod == null) {
      final Class<?> clazz = Class.forName(className, false, loader);
      myMethod = findMethod(clazz, myMethodName, myParamTypes);
    }
    return myMethod.invoke(null, paramValues);
  }

  private static Method findMethod(final Class<?> clazz, String name, Class<?>[] paramTypes) throws NoSuchMethodException {
    Method m = clazz.getDeclaredMethod(name, paramTypes);
    // speedup method invocation by calling setAccessible(true)
    m.setAccessible(true);
    return m;
  }
}
