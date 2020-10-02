/*
 * Copyright 2000-2020 JetBrains s.r.o.
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


import com.intellij.rt.coverage.assertEqualsClassLines
import com.intellij.rt.coverage.data.LineCoverage.FULL
import com.intellij.rt.coverage.runWithCoverage
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File


class KotlinCoverageStatusTest {
    private lateinit var myDataFile: File

    @Before
    fun setUp() {
        myDataFile = createTempFile("test")
    }

    @After
    fun tearDown() {
        myDataFile.delete()
    }

    @Test
    @Ignore("Not implemented")
    fun testDefaultArgs() = testClassCoverage("defaultArgs", mapOf(
            20 to FULL,
            24 to FULL,
            25 to FULL
    ))


    @Test
    @Ignore("Not implemented")
    fun testInline() = testClassCoverage("inline", mapOf(
            20 to FULL,
            21 to FULL,
            25 to FULL,
            26 to FULL
    ))

    @Test
    @Ignore("Not implemented")
    fun testInlineInline() = testClassCoverage("inlineInline", mapOf(
            20 to FULL,
            21 to FULL,
            22 to FULL,
            26 to FULL,
            27 to FULL,
            31 to FULL,
            32 to FULL
    ))

    @Test
    @Ignore("Not implemented")
    fun testReturn() = testClassCoverage("returnTest", mapOf(
            20 to FULL
    ))

    private fun testClassCoverage(testName: String, expected: Map<Int, Byte>, sampling: Boolean = true) {
        val project = runWithCoverage(myDataFile, testName, sampling)
        project.assertEqualsClassLines("kotlinTestData.$testName.TestKt", expected)
    }
}
