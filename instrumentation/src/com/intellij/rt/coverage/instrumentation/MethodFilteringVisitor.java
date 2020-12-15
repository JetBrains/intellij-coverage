/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.instrumentation.filters.FilterUtils;
import com.intellij.rt.coverage.instrumentation.filters.signature.MethodSignatureFilter;
import com.intellij.rt.coverage.util.StringsPool;
import org.jetbrains.coverage.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This visitor defines methods that should be visited.
 */
public class MethodFilteringVisitor extends ClassVisitor {
  private static final List<MethodSignatureFilter> ourSignatureFilters = FilterUtils.createSignatureFilters();

  private final String myClassName;
  private boolean myEnum = false;
  private boolean myHasInterfaces = false;
  private final List<String> myAnnotations = new ArrayList<String>();
  private HashMap<String, Object> myProperties;

  public MethodFilteringVisitor(ClassVisitor classVisitor, String className) {
    super(Opcodes.API_VERSION, classVisitor);
    myClassName = className;
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    myEnum = (access & Opcodes.ACC_ENUM) != 0;
    myHasInterfaces = interfaces != null && interfaces.length > 0;
    super.visit(version, access, name, signature, superName, interfaces);
  }

  /** Should be called only after first <code>visitMethod</code> has been called. */
  public boolean shouldInstrumentMethod(final int access,
                                        final String name,
                                        final String desc,
                                        final String signature,
                                        final String[] exceptions) {
    if ((access & Opcodes.ACC_BRIDGE) != 0) return false; //try to skip bridge methods
    if ((access & Opcodes.ACC_ABSTRACT) != 0)
      return false; //skip abstracts; do not include interfaces without non-abstract methods in result
    for (MethodSignatureFilter filter : ourSignatureFilters) {
      if (filter.shouldFilter(access, name, desc, signature, exceptions, this)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    myAnnotations.add(StringsPool.getFromPool(descriptor));
    return super.visitAnnotation(descriptor, visible);
  }

  public String getClassName() {
    return myClassName;
  }

  public boolean isEnum() {
    return myEnum;
  }

  public boolean hasInterfaces() {
    return myHasInterfaces;
  }

  public List<String> getAnnotations() {
    return myAnnotations;
  }

  public Object getProperty(String key) {
    createProperties();
    return myProperties.get(key);
  }

  public void addProperty(String key, Object value) {
    createProperties();
    myProperties.put(key, value);
  }

  private void createProperties() {
    if (myProperties == null) {
      myProperties = new HashMap<String, Object>();
    }
  }
}
