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

package com.intellij.rt.coverage;

import com.intellij.rt.coverage.data.BranchData;
import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.CoverageTransformer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CoverageNotNullInstrumentationTest {

  private void loadTransformed(String name, ProjectData data) throws Exception {
    final String resource = name.replace('.', '/') + ".class";
    ClassLoader loader = getClass().getClassLoader();
    byte[] bytes = TransformedClassLoader.readBytes(loader.getResourceAsStream(resource));
    byte[] transformedBytes = new CoverageTransformer(data, false)
        .instrument(bytes, name, loader, true);
    new TransformedClassLoader(getClass().getClassLoader(), name, transformedBytes).loadClass(name, true);
  }

  @Test
  public void testNotNullInstrumentation() throws Exception {
    String name = "WithNotNulls";
    ProjectData projectData = new ProjectData();
    loadTransformed(name, projectData);
    ClassData classData = projectData.getClassData(name);
    assertNotNull(classData);
    LineData[] lines = (LineData[]) classData.getLines();
    int lineCount = 0;
    for (LineData line : lines) {
      if (line != null) {
        lineCount++;
        BranchData branchData = line.getBranchData();
        if (branchData != null) {
          assertEquals(0, branchData.getTotalBranches());
        }
      }
    }
    assertEquals(4, lineCount);
  }
}
