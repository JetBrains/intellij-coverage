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

package com.intellij.rt.coverage.report;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.util.CoverageIOUtil;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.gnu.trove.TObjectIntHashMap;
import org.jetbrains.coverage.gnu.trove.TObjectIntProcedure;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Basic section that can store additional information per each class.
 */
public abstract class ClassListSection extends ReportSection {
  protected abstract void loadClass(DataInputStream in, ClassData classData, int version) throws IOException;

  protected abstract void saveClass(ClassData classData, DataOutput out, int index) throws IOException;


  @Override
  public void load(ProjectData projectData, DataInputStream in, TIntObjectHashMap<ClassData> dict, int version) throws IOException {
    int classId = CoverageIOUtil.readINT(in);
    while (classId != -1) {
      final ClassData classData = dict.get(classId);
      loadClass(in, classData, version);
      classId = CoverageIOUtil.readINT(in);
    }
  }

  @Override
  protected void saveInternal(final ProjectData projectData, final DataOutput out, TObjectIntHashMap<String> dict) throws IOException {
    try {
      dict.forEachEntry(new TObjectIntProcedure<String>() {
        public boolean execute(String className, int index) {
          try {
            saveClass(projectData.getClassData(className), out, index);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return true;
        }
      });
    } catch (RuntimeException e) {
      if (e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      } else {
        throw e;
      }
    }
    CoverageIOUtil.writeINT(out, -1);
  }
}
