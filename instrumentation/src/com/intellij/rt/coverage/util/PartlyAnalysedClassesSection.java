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
import com.intellij.rt.coverage.instrumentation.InstrumentationOptions;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

/**
 * During lines mapping new classes can be created, but they include only mapped lines.
 * The goal of this section is to save <code>ClassData#isFullyAnalysed</code> field
 * in order to treat this class as unloaded when collecting coverage results.
 *
 * @see <a href=https://youtrack.jetbrains.com/issue/IDEA-299956/>Issue example</a>
 */
public class PartlyAnalysedClassesSection extends ClassListSection {
  @Override
  protected void loadClass(DataInputStream in, ClassData classData, int version) throws IOException {
    if (classData != null) {
      classData.setFullyAnalysed(false);
    }
  }

  @Override
  protected void saveClass(ClassData classData, DataOutput out, int index) throws IOException {
    if (classData != null && !classData.isFullyAnalysed()) {
      CoverageIOUtil.writeINT(out, index);
    }
  }

  @Override
  public int getId() {
    return ReportSectionsUtil.PARTLY_ANALYSED_CLASSES_SECTION_ID;
  }

  @Override
  public int getVersion() {
    return 0;
  }

  @Override
  public boolean isEngaged(ProjectData projectData, InstrumentationOptions options) {
    for (ClassData classData : projectData.getClassesCollection()) {
      if (!classData.isFullyAnalysed()) return true;
    }
    return false;
  }
}
