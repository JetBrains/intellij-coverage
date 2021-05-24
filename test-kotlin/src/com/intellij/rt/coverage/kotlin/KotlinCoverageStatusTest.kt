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


import com.intellij.rt.coverage.*
import org.junit.After
import org.junit.Before
import java.io.File


abstract class KotlinCoverageStatusTest {
    protected lateinit var myDataFile: File

    @Before
    fun setUp() {
        myDataFile = createTempFile("test")
    }

    @After
    fun tearDown() {
        myDataFile.delete()
    }

    abstract val coverage: Coverage
    open val testTracking = false

    protected fun test(testName: String, vararg classes: String = arrayOf("TestKt"),
                       fileName: String = "test.kt", calcUnloaded: Boolean = false,
                       extraArgs: MutableList<String> = mutableListOf(),
                       patterns: String = "$TEST_PACKAGE.*") {
        val project = runWithCoverage(myDataFile, testName, coverage, calcUnloaded, testTracking, patterns, extraArgs)
        val testFile = pathToFile("src", "kotlinTestData", *testName.split('.').toTypedArray(), fileName)
        val fullClassNames = if (classes.contains(all)) listOf(all) else classes.map { "kotlinTestData.$testName.$it" }
        if (testTracking) {
            val expected = extractTestTrackingDataFromFile(testFile)
            assertEqualsTestTracking(myDataFile, expected, fullClassNames)
        } else {
            val expected = extractCoverageDataFromFile(testFile)
            assertEqualsLines(project, expected, fullClassNames)
        }
    }
}
