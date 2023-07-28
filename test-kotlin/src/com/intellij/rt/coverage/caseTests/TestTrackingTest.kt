/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package com.intellij.rt.coverage.caseTests

import com.intellij.rt.coverage.*
import com.intellij.rt.coverage.data.ProjectData
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import testData.custom.testTracking.parallelTests.CALLS_PER_LINE
import testData.custom.testTracking.sequentialTests.TESTS
import java.io.File

@RunWith(Parameterized::class)
internal class TestTrackingTest(
    override val coverage: Coverage,
    override val testTracking: TestTracking,
) : CoverageTest() {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} coverage with {1} test tracking")
        fun data() = allTestTrackingModes()
    }

    override fun verifyResults(projectData: ProjectData, configuration: TestConfiguration, testFile: File) {
        val expected = extractTestTrackingDataFromFile(testFile)
        assertEqualsTestTracking(myDataFile, expected, configuration.classes)
        assertEqualsLines(projectData, configuration.coverageData, configuration.classes)
    }

    @Test(timeout = 20000)
    fun testOneTest() = test("custom.testTracking.oneTest")

    @Test(timeout = 20000)
    fun testTwoTests() = test("custom.testTracking.twoTests")

    @Test(timeout = 20000)
    fun testManyTests() {
        val test = getTestFile("custom.testTracking.parallelTests")
        test(
            test.testName,
            configuration = extractTestConfiguration(test.file).copy(extraArgs = mutableListOf("-Dthreads=1"))
        ) { projectData, configuration, _ ->
            val lines = testTrackingLines(myDataFile, configuration.classes)
            Assert.assertEquals(5, lines.size)
            lines.values.forEach { Assert.assertEquals(CALLS_PER_LINE, it.size) }
            assertEqualsLines(projectData, configuration.coverageData, configuration.classes)
        }
    }

    @Test(timeout = 20000)
    fun testThreadSafeStructure() {
        val test = getTestFile("custom.testTracking.parallelTests")
        test(
            test.testName,
            configuration = extractTestConfiguration(test.file).copy(extraArgs = mutableListOf("-Dthreads=2"))
        ) { projectData, configuration, _ ->
            testTrackingLines(myDataFile, configuration.classes)
            assertEqualsLines(projectData, configuration.coverageData, configuration.classes)
        }
    }

    @Test(timeout = 20000)
    fun testSequentialTests() = test("custom.testTracking.sequentialTests") { projectData, configuration, _ ->
        val lines = testTrackingLines(myDataFile, configuration.classes)
        Assert.assertEquals(5, lines.size)
        lines.values.forEach { Assert.assertEquals(TESTS, it.size) }
        assertEqualsLines(projectData, configuration.coverageData, configuration.classes)
    }
}

@RunWith(Parameterized::class)
internal class TestTrackingVerifyInstrumentationTest(
    override val coverage: Coverage,
    override val testTracking: TestTracking,
) : CoverageTest() {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} coverage with {1} test tracking")
        fun data() = allTestTrackingModes()
    }

    override fun verifyResults(projectData: ProjectData, configuration: TestConfiguration, testFile: File) {
        // just check that instrumentation does not cause runtime issues
    }

    @Test
    fun testBadCycleClasses() = test("badCycle.classes")

    @Test
    fun testBadCycleInterfaces() = test("badCycle.interfaces")

    @Test
    fun testCoroutinesInline() = test("coroutines.inline")
}

private fun allTestTrackingModes() = Coverage.valuesWithCondyWhenPossible().toList()
    .product(TestTracking.values().toList())
    .map { it.toList().toTypedArray() }.toTypedArray()
