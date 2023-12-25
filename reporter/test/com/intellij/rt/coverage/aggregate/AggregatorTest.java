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

package com.intellij.rt.coverage.aggregate;

import com.intellij.rt.coverage.aggregate.api.AggregatorApi;
import com.intellij.rt.coverage.aggregate.api.Request;
import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.report.TestUtils;
import com.intellij.rt.coverage.report.data.BinaryReport;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class AggregatorTest {

  @Test
  public void testSingleReport() throws IOException {
    List<Request> requests = createRequests();
    runAggregator(requests, "testData.defaultArgs.TestKt");
  }

  @Test
  public void testSeveralReport() throws IOException {
    List<Request> requests = createRequests();
    runAggregator(requests, "testData.defaultArgs.TestKt", "testData.branches.TestKt", "testData.crossinline.TestKt");
  }

  @NotNull
  private static List<Request> createRequests() throws IOException {
    List<Request> requests = new ArrayList<Request>();
    Request request1 = new Request(
        TestUtils.createFilters(Pattern.compile("testData\\..*"), Pattern.compile("testData\\.inline")),
        File.createTempFile("request", "ic"), null
    );
    Request request2 = new Request(
        TestUtils.createFilters(Pattern.compile(".*inline.*"), Pattern.compile(".*ss.*")),
        File.createTempFile("request", "ic"), null
    );
    requests.add(request1);
    requests.add(request2);
    return requests;
  }


  public static void runAggregator(List<Request> requests, String... testsClasses) {
    List<File> reports = new ArrayList<File>();
    for (String test : testsClasses) {
      BinaryReport report = TestUtils.runTest("", test);
      reports.add(report.getDataFile());
    }

    TestUtils.clearLogFile(new File("."));
    AggregatorApi.aggregate(requests, reports, TestUtils.getOutputRoots());
    TestUtils.checkLogFile(new File("."));

    List<ProjectData> projectDataList = new ArrayList<ProjectData>();
    Set<String> names = new HashSet<String>();
    for (Request request : requests) {
      File file = request.outputFile;
      ProjectData projectData = ProjectDataLoader.load(file);
      for (ClassData classData : projectData.getClassesCollection()) {
        names.add(classData.getName());
      }
      projectDataList.add(projectData);
    }

    for (int i = 0; i < requests.size(); i++) {
      Request request = requests.get(i);
      ProjectData projectData = projectDataList.get(i);
      for (String name : names) {
        Assert.assertEquals(request.classFilter.shouldInclude(name), projectData.getClassData(name) != null);
      }
    }
  }
}
