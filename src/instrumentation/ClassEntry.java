package com.intellij.rt.coverage.instrumentation;

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
