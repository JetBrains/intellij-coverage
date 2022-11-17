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

package com.intellij.rt.coverage;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.offline.RawReportLoader;
import com.intellij.rt.coverage.offline.RawHitsReport;
import com.intellij.rt.coverage.offline.RawProjectData;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

public class RawHitsReportTest {
  @Test
  public void testReportSave() throws Throwable {
    final RawProjectData rawProjectData = new RawProjectData();
    final int[] hits = rawProjectData.createClassData("A", 2).hits;
    hits[0] = 2;
    hits[1] = 3;

    final File file = Files.createTempFile("coverage", "ric").toFile();
    RawHitsReport.dump(file, rawProjectData);

    final ProjectData projectData = createProject();
    final ClassData classData = projectData.getClassData("A");
    classData.setHitsMask(hits);
    classData.applyHits();

    final ProjectData loadedProjectData = createProject();
    RawReportLoader.load(file, loadedProjectData);

    final ClassData loadedClassData = loadedProjectData.getClassData("A");
    for (int i = 0; i < classData.getLines().length; i++) {
      Assert.assertEquals(classData.getLineData(i).getHits(), loadedClassData.getLineData(i).getHits());
    }
  }

  private static ProjectData createProject() {
    final ProjectData projectData = ProjectData.createProjectData(false);
    final ClassData classData = projectData.getOrCreateClassData("A");
    classData.setLines(new LineData[]{
        new LineData(0, "a()"),
        new LineData(1, "a()"),
    });
    return projectData;
  }
}
