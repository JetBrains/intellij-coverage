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

import com.intellij.rt.coverage.Coverage
import com.intellij.rt.coverage.createTmpFile
import com.intellij.rt.coverage.runWithCoverage
import com.intellij.rt.coverage.util.diff.CoverageDiff
import org.junit.Assert
import org.junit.Test

internal class NewInstrumentationTest {

    @Test
    fun testNewLineCoverageJoda() = testCoverageJoda(Coverage.LINE, Coverage.NEW_LINE)

    @Test
    fun testNewBranchCoverageJoda() = testCoverageJoda(Coverage.BRANCH, Coverage.NEW_BRANCH)

    private fun testCoverageJoda(before: Coverage, after: Coverage) {
        val exclude = listOf( // exclude non deterministic classes from coverage
            "org\\.joda\\.time\\.TestDateTimeZone\\$4",
            "org\\.joda\\.time\\.convert.ConverterSet",
            "org\\.joda\\.time\\.chrono\\.gj\\.MainTest"
        )
        val patterns = "org\\.joda\\.time.* -exclude ${exclude.joinToString(" ")}"
        assertEqualCoverage(before, after, "custom.newInstrumentation.joda", patterns)
    }

    private fun assertEqualCoverage(before: Coverage, after: Coverage, testName: String, patterns: String) {
        val (fileA, fileB) = List(2) { createTmpFile(".ic") }.onEach { it.deleteOnExit() }
        val projectA = runWithCoverage(fileA, testName, before, patterns = patterns)
        val projectB = runWithCoverage(fileB, testName, after, patterns = patterns)
        val diff = CoverageDiff.coverageDiff(projectA, projectB)

        Assert.assertTrue(diff.classesDiff.toString(), diff.classesDiff.isEmpty())
        Assert.assertTrue(diff.linesDiff.toString(), diff.linesDiff.isEmpty())
        Assert.assertTrue(diff.jumpsDiff.toString(), diff.jumpsDiff.isEmpty())
        Assert.assertTrue(diff.switchesDiff.toString(), diff.switchesDiff.isEmpty())
    }
}
