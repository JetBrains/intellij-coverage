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
import com.intellij.rt.coverage.util.RawHitsReport;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.regex.Pattern;

public class RawHitsReportTest {
  @Test
  public void testReportSave() throws Throwable {
    final ProjectData projectData = createProject();
    final ClassData classData = projectData.getClassData("A");
    classData.createHitsMask(2);
    final int[] hits = classData.getHitsMask();
    hits[0] = 2;
    hits[1] = 3;

    final File file = Files.createTempFile("coverage", "ric").toFile();
    RawHitsReport.dump(file, projectData);
    classData.applyLinesMask();

    final ProjectData loadedProjectData = createProject();
    RawHitsReport.load(file, loadedProjectData);

    final ClassData loadedClassData = loadedProjectData.getClassData("A");
    for (int i = 0; i < classData.getLines().length; i++) {
      Assert.assertEquals(classData.getLineData(i).getHits(), loadedClassData.getLineData(i).getHits());
    }
  }

  private static ProjectData createProject() throws IOException {
    final ProjectData projectData = ProjectData.createProjectData(null, null, false, true, Collections.<Pattern>emptyList(), Collections.<Pattern>emptyList(), null);
    final ClassData classData = projectData.getOrCreateClassData("A");
    classData.setLines(new LineData[]{
        new LineData(0, "a()"),
        new LineData(1, "a()"),
    });
    return projectData;
  }
}
