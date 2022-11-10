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

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.JumpData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.CoverageTransformer;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CoverageNotNullInstrumentationTest {

  @Before
  public void setUp() {
    System.setProperty("idea.new.sampling.coverage", "true"); 
  }

  @After
  public void tearDown() {
    System.clearProperty("idea.new.sampling.coverage");
  }

  private byte[] doTransform(final String name, final ProjectData data) throws IOException {
    final String resource = name.replace('.', '/') + ".class";
    ClassLoader loader = WithNotNulls.class.getClassLoader();
    byte[] bytes = TransformedClassLoader.readBytes(loader.getResourceAsStream(resource));
    return doTransform(name, bytes, loader, data);
  }

  private byte[] doTransform(String name, byte[] bytes, ClassLoader loader, final ProjectData data) {
    return new CoverageTransformer(data, false, Collections.<Pattern>emptyList(), Collections.<Pattern>emptyList())
        .instrument(bytes, name, loader, true);
  }

  @Test
  public void testNotNullInstrumentation() throws Exception {
    String name = WithNotNulls.class.getName();
    ProjectData projectData = new ProjectData();
    new TransformedClassLoader(WithNotNulls.class.getClassLoader(), name, doTransform(name, projectData)).loadClass(name, true);
    ClassData classData = projectData.getClassData(name);
    assertNotNull(classData);
    Object[] lines = classData.getLines();
    int jumpsCount = 0;
    for (Object line : lines) {
      if (line != null) {
        JumpData[] jumps = ((LineData) line).getJumps();
        if (jumps != null && jumps.length > 0) {
          jumpsCount++;
        }
      }
    }
    assertEquals(0, jumpsCount);
  }


  static class WithNotNulls {
    @NotNull
    public String foo(@NotNull String s) {
      return "";
    }
  }
}
