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

package com.intellij.rt.coverage.util;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.ProjectData;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.gnu.trove.TObjectIntHashMap;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class IncludePatternsSection extends ReportSection {
  @Override
  public int getId() {
    return ReportSectionsUtil.INCLUDE_PATTERNS_SECTION_ID;
  }

  @Override
  public int getVersion() {
    return 0;
  }

  @Override
  public boolean isEngaged(ProjectData projectData) {
    return true;
  }

  @Override
  public void load(ProjectData projectData, DataInputStream in, TIntObjectHashMap<ClassData> dict, int version) throws IOException {
    projectData.setIncludePatterns(readPatterns(in));
    projectData.setExcludePatterns(readPatterns(in));
    projectData.setAnnotationsToIgnore(readPatterns(in));
  }

  @Override
  protected void saveInternal(ProjectData projectData, DataOutput out, TObjectIntHashMap<String> dict) throws IOException {
    dumpPatterns(out, projectData.getIncudePatterns());
    dumpPatterns(out, projectData.getExcludePatterns());
    dumpPatterns(out, projectData.getAnnotationsToIgnore());
  }

  private void dumpPatterns(DataOutput out, List<Pattern> patterns) throws IOException {
    int size = patterns == null ? 0 : patterns.size();
    CoverageIOUtil.writeINT(out, size);
    if (size == 0) return;
    for (Pattern pattern : patterns) {
      CoverageIOUtil.writeUTF(out, pattern.pattern());
    }
  }

  private List<Pattern> readPatterns(DataInputStream in) throws IOException {
    int size = CoverageIOUtil.readINT(in);
    List<Pattern> result = new ArrayList<Pattern>();
    for (int i = 0; i < size; i++) {
      String pattern = CoverageIOUtil.readUTFFast(in);
      result.add(Pattern.compile(pattern));
    }
    return result;
  }
}
