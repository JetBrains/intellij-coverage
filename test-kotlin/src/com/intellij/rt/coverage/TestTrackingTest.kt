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

package com.intellij.rt.coverage

import com.intellij.rt.coverage.data.ProjectData
import org.junit.Assert
import org.junit.Test
import testData.custom.testTracking.parallelTests.CALLS_PER_LINE
import testData.custom.testTracking.sequentialTests.TESTS
import java.io.File

internal abstract class AbstractTestTrackingTest(override val coverage: Coverage) : CoverageTest() {
    override val testTracking = true

    override fun verifyResults(projectData: ProjectData, configuration: TestConfiguration, testFile: File) {
        val expected = extractTestTrackingDataFromFile(testFile)
        assertEqualsTestTracking(myDataFile, expected, configuration.classes)
    }

    @Test
    fun testOneTest() = test("custom.testTracking.oneTest")

    @Test
    fun testTwoTests() = test("custom.testTracking.twoTests")

    @Test
    fun testManyTests() {
        val test = getTestFile("custom.testTracking.parallelTests")
        test(
            test.testName,
            configuration = extractTestConfiguration(test.file).copy(extraArgs = mutableListOf("-Dthreads=1"))
        ) { _, config, _ ->
            val lines = testTrackingLines(myDataFile, config.classes)
            Assert.assertEquals(5, lines.size)
            lines.values.forEach { Assert.assertEquals(CALLS_PER_LINE, it.size) }
        }
    }

    @Test
    fun testThreadSafeStructure() {
        val test = getTestFile("custom.testTracking.parallelTests")
        test(
            test.testName,
            configuration = extractTestConfiguration(test.file).copy(extraArgs = mutableListOf("-Dthreads=2"))
        ) { _, config, _ -> testTrackingLines(myDataFile, config.classes) }
    }

    @Test
    fun testSequentialTests() = test("custom.testTracking.sequentialTests") { _, config, _ ->
        val lines = testTrackingLines(myDataFile, config.classes)
        Assert.assertEquals(5, lines.size)
        lines.values.forEach { Assert.assertEquals(TESTS, it.size) }
    }
}

internal class TestTrackingTracingTest : AbstractTestTrackingTest(Coverage.TRACING)
internal class TestTrackingNewTracingTest : AbstractTestTrackingTest(Coverage.NEW_TRACING)
internal class ClassDataTestTrackingNewTracingTest : AbstractTestTrackingTest(Coverage.NEW_TRACING) {
    init {
        commonExtraArgs.add("-Didea.new.test.tracking.coverage=false")
    }
}

/**
 * Inheritors of this class check that running coverage with test tracking works OK on the test corpus.
 */
internal abstract class TestTrackingVerifyInstrumentationTest(override val coverage: Coverage) : CoverageRunTest() {
    override val testTracking = true
    override fun verifyResults(projectData: ProjectData, configuration: TestConfiguration, testFile: File) {
        // just check that instrumentation does not cause runtime issues
    }
}

internal class TestTrackingTracingCoverageTest : TestTrackingVerifyInstrumentationTest(Coverage.TRACING)
internal class TestTrackingNewTracingCoverageTest : TestTrackingVerifyInstrumentationTest(Coverage.NEW_TRACING)
internal class ClassDataTestTrackingNewTracingCoverageTest : TestTrackingVerifyInstrumentationTest(Coverage.NEW_TRACING) {
    init {
        commonExtraArgs.add("-Didea.new.test.tracking.coverage=false")
    }
}
