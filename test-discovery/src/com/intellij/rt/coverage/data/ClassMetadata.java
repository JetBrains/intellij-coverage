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

package com.intellij.rt.coverage.data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ClassMetadata {
  private String fqn;
  private List<String> files;
  private Map<String, byte[]> methods;

  public ClassMetadata() {
    this("", Collections.<String>emptyList(), Collections.<String, byte[]>emptyMap());
  }

  public ClassMetadata(String fqn, List<String> files, Map<String, byte[]> methods) {
    assert fqn != null;
    this.setFqn(fqn);
    this.setFiles(files);
    this.setMethods(methods);
  }

  public String getFqn() {
    return fqn;
  }

  public void setFqn(String fqn) {
    this.fqn = fqn;
  }

  public List<String> getFiles() {
    return files;
  }

  public void setFiles(List<String> files) {
    this.files = files;
  }

  public Map<String, byte[]> getMethods() {
    return methods;
  }

  public void setMethods(Map<String, byte[]> methods) {
    this.methods = methods;
  }
}
