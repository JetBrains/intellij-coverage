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
package com.intellij.rt.coverage.aggregate

import com.intellij.rt.coverage.aggregate.api.AggregatorApi
import com.intellij.rt.coverage.aggregate.api.Request
import com.intellij.rt.coverage.data.ProjectData
import com.intellij.rt.coverage.report.TestUtils.checkLogFile
import com.intellij.rt.coverage.report.TestUtils.clearLogFile
import com.intellij.rt.coverage.report.TestUtils.createFilters
import com.intellij.rt.coverage.report.TestUtils.outputRoots
import com.intellij.rt.coverage.report.TestUtils.runTest
import com.intellij.rt.coverage.util.ProjectDataLoader
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

class AggregatorTest {
    @Test
    fun testSingleReport() {
        val requests = createRequests()
        runAggregator(requests, "testData.defaultArgs.TestKt")
    }

    @Test
    fun testSeveralReport() {
        val requests = createRequests()
        runAggregator(
            requests,
            "testData.defaultArgs.TestKt",
            "testData.branches.TestKt",
            "testData.crossinline.TestKt"
        )
    }

    @Test
    fun testMergeReport() {
        val report1 = runTest("", "testData.defaultArgs.TestKt").dataFile
        val report2 = runTest("", "testData.branches.TestKt").dataFile

        val mergedReport = File.createTempFile("merged", "ic")

        AggregatorApi.merge(listOf(report1, report2), mergedReport)

        val projectData = ProjectDataLoader.load(mergedReport)
        Assert.assertNotNull(projectData.getClassData("testData.defaultArgs.TestKt"))
        Assert.assertNotNull(projectData.getClassData("testData.branches.TestKt"))
    }

    companion object {
        private fun createRequests(): List<Request> {
            val requests: MutableList<Request> = ArrayList()
            val request1 = Request(
                createFilters(Pattern.compile("testData\\..*"), Pattern.compile("testData\\.inline")),
                File.createTempFile("request", "ic"), null
            )
            val request2 = Request(
                createFilters(Pattern.compile(".*inline.*"), Pattern.compile(".*ss.*")),
                File.createTempFile("request", "ic"), null
            )
            requests.add(request1)
            requests.add(request2)
            return requests
        }


        fun runAggregator(requests: List<Request>, vararg testsClasses: String?) {
            val reports: MutableList<File> = ArrayList()
            for (test in testsClasses) {
                val report = runTest("", test)
                reports.add(report.dataFile)
            }

            clearLogFile(File("."))
            AggregatorApi.aggregate(requests, reports, outputRoots)
            checkLogFile(File("."))

            val projectDataList: MutableList<ProjectData> = ArrayList()
            val names: MutableSet<String> = HashSet()
            for (request in requests) {
                val file = request.outputFile
                val projectData = ProjectDataLoader.load(file)
                for (classData in projectData.classesCollection) {
                    names.add(classData.name)
                }
                projectDataList.add(projectData)
            }

            for (i in requests.indices) {
                val request = requests[i]
                val projectData = projectDataList[i]
                for (name in names) {
                    Assert.assertEquals(request.classFilter.shouldInclude(name), projectData.getClassData(name) != null)
                }
            }
        }
    }
}
