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

package com.intellij.rt.coverage.util.classFinder;

import java.io.InputStream;

/**
 * @author Pavel.Sher
 */
public class ClassEntry {
  private String myClassName;
  private ClassLoader myClassLoader;

  public ClassEntry(final String className, final ClassLoader classLoader) {
    myClassName = className;
    myClassLoader = classLoader;
  }

  public String getClassName() {
    return myClassName;
  }

  public InputStream getClassInputStream() {
    String resourceName = myClassName.replace('.', '/') + ".class";
    InputStream is = getResourceStream(resourceName);
    if (is != null) return is;
    return getResourceStream("/" + resourceName);
  }

  private InputStream getResourceStream(final String resourceName) {
    if (myClassLoader == null) {
      return getClass().getResourceAsStream(resourceName);
    }

    return myClassLoader.getResourceAsStream(resourceName);
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ClassEntry that = (ClassEntry) o;

    if (myClassLoader != null ? !myClassLoader.equals(that.myClassLoader) : that.myClassLoader != null) return false;
    if (!myClassName.equals(that.myClassName)) return false;

    return true;
  }

  public int hashCode() {
    int result = myClassName.hashCode();
    result = 31 * result + (myClassLoader != null ? myClassLoader.hashCode() : 0);
    return result;
  }
}
