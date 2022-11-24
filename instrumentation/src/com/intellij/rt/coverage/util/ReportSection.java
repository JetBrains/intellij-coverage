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

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.ProjectData;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.gnu.trove.TObjectIntHashMap;

import java.io.*;

/**
 * A report section is an extra data that is written to the report file after the main part.
 */
public abstract class ReportSection {
  /**
   * Unique identifier of the section.
   * If the identifier is unknown, the section data is skipped.
   */
  public abstract int getId();

  /**
   * Sections perform independent versioning to support individual section skipping.
   * If the version is newer that current support version, the section data is skipped.
   */
  public abstract int getVersion();

  /**
   * Check if this section should be added to the report.
   */
  public abstract boolean isEngaged(ProjectData projectData);

  /**
   * Load data that was previously saved via <code>saveInternal</code> method.
   *
   * @param version version of the section in the agent that generated the report, may be lower than the current version
   */
  public abstract void load(ProjectData projectData, DataInputStream in, TIntObjectHashMap<ClassData> dict, int version) throws IOException;

  protected abstract void saveInternal(ProjectData projectData, DataOutput out, TObjectIntHashMap<String> dict) throws IOException;

  /**
   * Save section data. Identifier, size and version are saved before the data.
   */
  public final void save(ProjectData projectData, DataOutputStream out, TObjectIntHashMap<String> dict) throws IOException {
    final ByteArrayOutputStream tmpOut = new ByteArrayOutputStream();
    saveInternal(projectData, new DataOutputStream(tmpOut), dict);
    CoverageIOUtil.writeINT(out, getId());
    CoverageIOUtil.writeINT(out, tmpOut.size());
    CoverageIOUtil.writeINT(out, getVersion());
    tmpOut.writeTo(out);
  }
}
