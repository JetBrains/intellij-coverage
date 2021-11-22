/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.ProjectData;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.gnu.trove.TObjectIntHashMap;

import java.io.*;

public abstract class ReportSection {
  public abstract void load(ProjectData projectData, DataInputStream in, TIntObjectHashMap<ClassData> dict) throws IOException;
  public abstract int getId();
  public abstract boolean isEngaged(ProjectData projectData);
  protected abstract void saveInternal(ProjectData projectData, DataOutput out, TObjectIntHashMap<String> dict) throws IOException;

  public final void save(ProjectData projectData, DataOutputStream out, TObjectIntHashMap<String> dict) throws IOException {
    final ByteArrayOutputStream tmpOut = new ByteArrayOutputStream();
    saveInternal(projectData, new DataOutputStream(tmpOut), dict);
    CoverageIOUtil.writeINT(out, getId());
    CoverageIOUtil.writeINT(out, tmpOut.size());
    tmpOut.writeTo(out);
  }
}
