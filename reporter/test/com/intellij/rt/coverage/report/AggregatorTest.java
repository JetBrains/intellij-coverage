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

import com.intellij.rt.coverage.aggregate.Aggregator;
import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.report.data.BinaryReport;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import com.intellij.rt.coverage.util.classFinder.ClassFilter;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class AggregatorTest {

  @Test
  public void testSingleReport() throws IOException, InterruptedException {
    final List<Aggregator.Request> requests = createRequests();
    runAggregator(requests, "testData.defaultArgs.TestKt");
  }

  @Test
  public void testSeveralReport() throws IOException, InterruptedException {
    final List<Aggregator.Request> requests = createRequests();
    runAggregator(requests, "testData.defaultArgs.TestKt", "testData.branches.TestKt", "testData.crossinline.TestKt");
  }

  @NotNull
  private static List<Aggregator.Request> createRequests() throws IOException {
    final List<Aggregator.Request> requests = new ArrayList<Aggregator.Request>();
    final Aggregator.Request request1 = new Aggregator.Request(
        new ClassFilter.PatternFilter(
            Collections.singletonList(Pattern.compile("testData\\..*")),
            Collections.singletonList(Pattern.compile("testData\\.inline"))),
        File.createTempFile("request", "ic")
    );
    final Aggregator.Request request2 = new Aggregator.Request(
        new ClassFilter.PatternFilter(
            Collections.singletonList(Pattern.compile(".*inline.*")),
            Collections.singletonList(Pattern.compile(".*ss.*"))),
        File.createTempFile("request", "ic")
    );
    requests.add(request1);
    requests.add(request2);
    return requests;
  }


  public static void runAggregator(List<Aggregator.Request> requests, String... testsClasses) throws IOException, InterruptedException {
    final List<BinaryReport> reports = new ArrayList<BinaryReport>();
    for (String test : testsClasses) {
      final BinaryReport report = TestUtils.runTest("", test);
      reports.add(report);
    }

    new Aggregator(reports, TestUtils.getModules(), requests).processRequests();

    final List<ProjectData> projectDataList = new ArrayList<ProjectData>();
    final Set<String> names = new HashSet<String>();
    for (Aggregator.Request request : requests) {
      final File file = request.outputFile;
      final ProjectData projectData = ProjectDataLoader.load(file);
      for (ClassData classData : projectData.getClassesCollection()) {
        names.add(classData.getName());
      }
      projectDataList.add(projectData);
    }

    for (int i = 0; i < requests.size(); i++) {
      final Aggregator.Request request = requests.get(i);
      final ProjectData projectData = projectDataList.get(i);
      for (String name : names) {
        Assert.assertEquals(request.classFilter.shouldInclude(name), projectData.getClassData(name) != null);
      }
    }
  }
}
