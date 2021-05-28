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

package com.intellij.rt.coverage.kotlin

import com.intellij.rt.coverage.Coverage
import com.intellij.rt.coverage.TEST_PACKAGE
import com.intellij.rt.coverage.runWithCoverage
import com.intellij.rt.coverage.testTrackingLines
import kotlinTestData.testTracking.parallelTests.CALLS_PER_LINE
import kotlinTestData.testTracking.sequentialTests.TESTS
import org.junit.Assert
import org.junit.Test

internal abstract class KotlinAbstractTestTrackingTest : KotlinCoverageStatusTest() {
    override val testTracking = true

    @Test
    fun testOneTest() = test("testTracking.oneTest")

    @Test
    fun testTwoTests() = test("testTracking.twoTests")

    @Test
    fun testManyTests() = testManyTests(1)

    @Test
    fun testThreadSafeStructure() = testManyTests(2)

    private fun testManyTests(threads: Int) {
        val testName = "testTracking.parallelTests"
        runWithCoverage(myDataFile, testName, coverage, testTracking = true, patterns = "$TEST_PACKAGE.*", extraArgs = mutableListOf("-Dthreads=$threads"))
        val fullClassNames = listOf("Class0", "Class1", "Class2", "Class3", "Class4").map { "kotlinTestData.$testName.$it" }
        val lines = testTrackingLines(myDataFile, fullClassNames)
        if (threads == 1) {
            Assert.assertEquals(5, lines.size)
            lines.values.forEach { Assert.assertEquals(CALLS_PER_LINE, it.size) }
        }
    }

    @Test
    fun testSequentialTests() {
        val testName = "testTracking.sequentialTests"
        runWithCoverage(myDataFile, testName, coverage, testTracking = true, patterns = "$TEST_PACKAGE.*")
        val fullClassNames = listOf("Class0", "Class1", "Class2", "Class3", "Class4").map { "kotlinTestData.$testName.$it" }
        val lines = testTrackingLines(myDataFile, fullClassNames)
        Assert.assertEquals(5, lines.size)
        lines.values.forEach { Assert.assertEquals(TESTS, it.size) }
    }
}

internal class KotlinTestTrackingTracingTest : KotlinAbstractTestTrackingTest() {
    override val coverage = Coverage.TRACING
}

internal class KotlinTestTrackingNewTracingTest : KotlinAbstractTestTrackingTest() {
    override val coverage = Coverage.NEW_TRACING
}
