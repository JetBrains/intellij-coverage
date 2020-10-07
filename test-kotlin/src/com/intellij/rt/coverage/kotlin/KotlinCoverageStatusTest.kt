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
import com.intellij.rt.coverage.data.LineCoverage.*
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
    fun testDefaultArgsCovered() = testClassCoverage("defaultArgs.covered", mapOf(
            19 to FULL,
            20 to FULL,
            24 to FULL,
            25 to FULL
    ))

    @Test
    fun testDefaultArgsUncovered() = testClassCoverage("defaultArgs.uncovered", mapOf(
            19 to NONE,
            20 to FULL,
            24 to FULL,
            25 to FULL
    ))

    @Test
    fun testDefaultArgsSeveralArgs() = testClassCoverage("defaultArgs.severalArguments", mapOf(
            20 to NONE,
            21 to FULL,
            23 to FULL,
            27 to FULL,
            28 to FULL
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

    @Test
    fun testFileProperties() = testClassCoverage("properties.file", mapOf(
            // line 20 is invisible for coverage as property is const
            21 to FULL, // value is written in <cinit>
            22 to NONE, // getter and setter are uncovered
            23 to FULL, // value is written in <cinit>
            // line 25 is invisible for coverage as property is private
            26 to FULL // value is written in <cinit>
    ))

    @Test
    @Ignore("Not implemented")
    fun testGetterAndSetterOfPropertyAreDistinguishable() = testClassCoverage("properties.getter_setter", mapOf(
            20 to PARTIAL, // setter is not covered
            21 to PARTIAL // getter is not covered
    ))

    @Test
    fun testPrimaryConstructorWithProperties() = testClassCoverage("properties.constructor", mapOf(
            20 to FULL,
            // line 21 is invisible for coverage as property is private
            22 to NONE // getter is not covered
    ), className = "kotlinTestData.properties.constructor.A")

    @Test
    fun testDataClass() = testClassCoverage("dataClass", mapOf(
            19 to FULL,
            21 to FULL,
            24 to FULL,
            25 to FULL
    ), className = "kotlinTestData.dataClass.A")

    private fun testClassCoverage(testName: String, expected: Map<Int, Byte>, sampling: Boolean = true,
                                  className: String = "kotlinTestData.$testName.TestKt") {
        val project = runWithCoverage(myDataFile, testName, sampling)
        assertEqualsClassLines(project, className, expected)
    }
}
