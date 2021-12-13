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

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Pavel.Sher
 */
public abstract class ClassEntry {
  private final String myClassName;

  public ClassEntry(final String className) {
    myClassName = className;
  }

  public String getClassName() {
    return myClassName;
  }

  public abstract InputStream getClassInputStream() throws IOException;

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final ClassEntry that = (ClassEntry) o;
    return myClassName.equals(that.myClassName);
  }

  public int hashCode() {
    return myClassName.hashCode();
  }

  public interface Consumer {
    void consume(ClassEntry classEntry);
  }
}
