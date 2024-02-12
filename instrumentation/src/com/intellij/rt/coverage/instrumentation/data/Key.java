/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation.data;

import com.intellij.rt.coverage.data.ProjectData;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;

import java.util.List;
import java.util.Set;

@SuppressWarnings({"InstantiationOfUtilityClass"})
public class Key<T> {
  public static final Key<ProjectData> PROJECT_DATA = new Key<ProjectData>();

  public static final Key<ClassReader> CLASS_READER = new Key<ClassReader>();
  public static final Key<String> CLASS_NAME = new Key<String>();
  public static final Key<String> CLASS_INTERNAL_NAME = new Key<String>();
  public static final Key<Integer> CLASS_ACCESS = new Key<Integer>();
  public static final Key<String[]> INTERFACES = new Key<String[]>();

  public static final Key<Integer> METHOD_ACCESS = new Key<Integer>();
  public static final Key<String> METHOD_NAME = new Key<String>();
  public static final Key<String> METHOD_DESC = new Key<String>();
  public static final Key<String> METHOD_SIGNATURE = new Key<String>();
  public static final Key<String[]> EXCEPTIONS = new Key<String[]>();
  public static final Key<List<String>> METHOD_ANNOTATIONS = new Key<List<String>>();


  public static final Key<Boolean> IS_KOTLIN = new Key<Boolean>();
  public static final Key<Boolean> IS_SEALED_CLASS = new Key<Boolean>();
}
