/*
 * Copyright 2000-2026 JetBrains s.r.o.
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

import com.intellij.rt.coverage.data.ProjectData;
import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public class ProjectDataLoaderTest extends TestCase {
  public void testLoadDoesNotCloseInputStream() throws IOException {
    final TrackingInputStream inputStream = new TrackingInputStream(createEmptyProjectDataBytes());

    final ProjectData projectData = ProjectDataLoader.load(inputStream);

    assertFalse("Input stream should not be closed by ProjectDataLoader.load", inputStream.isClosed());
    assertEquals(0, projectData.getClassesNumber());
  }

  public void testLoadEmptyInputStream() {
    final TrackingInputStream inputStream = new TrackingInputStream(new byte[0]);

    final ProjectData projectData = ProjectDataLoader.load(inputStream);

    assertFalse("Input stream should not be closed by ProjectDataLoader.load", inputStream.isClosed());
    assertEquals(0, projectData.getClassesNumber());
  }

  public void testLoadFile() throws IOException {
    final File tempFile = File.createTempFile("project_data", "ideacovtest");
    writeEmptyProjectData(tempFile);

    final ProjectData projectData = ProjectDataLoader.load(tempFile);

    assertEquals(0, projectData.getClassesNumber());
  }

  public void testLoadEmptyFile() throws IOException {
    final File tempFile = File.createTempFile("project_data", "ideacovtest");

    final ProjectData projectData = ProjectDataLoader.load(tempFile);

    assertEquals(0, projectData.getClassesNumber());
  }

  private static byte[] createEmptyProjectDataBytes() throws IOException {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    final DataOutputStream out = new DataOutputStream(bytes);
    CoverageIOUtil.writeINT(out, 0);
    out.flush();
    return bytes.toByteArray();
  }

  private static void writeEmptyProjectData(File file) throws IOException {
    DataOutputStream out = null;
    try {
      out = CoverageIOUtil.openWriteFile(file);
      CoverageIOUtil.writeINT(out, 0);
    } finally {
      CoverageIOUtil.close(out);
    }
  }

}
