/*
 * Copyright 2000-2024 JetBrains s.r.o.
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
import com.intellij.rt.coverage.util.ErrorReporter
import org.junit.After
import org.junit.Assert
import org.junit.Before
import java.io.File

/** Root package for test data. */
const val TEST_PACKAGE = "testData"

/** Test source file info. */
class TestFile(val testName: String, val file: File, val mainClass: String)

/**
 * Test configuration data.
 * Usually it is extracted from source file.
 * @see [extractTestConfiguration]
 */
data class TestConfiguration(
    val classes: List<String> = emptyList(),
    val patterns: String? = null,
    val extraArgs: MutableList<String> = mutableListOf(),
    val calculateUnloaded: Boolean = false,
    val expectedClasses: List<String>? = null,
    val fileWithMarkers: File? = null,
    val coverageData: Map<Int, String>? = null,
)

internal const val LOG_NAME = ErrorReporter.ERROR_FILE
private fun File.ifExists() = takeIf { it.exists() }
internal fun logFile(dataFile: File) = File(dataFile.parent, LOG_NAME).ifExists()
    ?: File(LOG_NAME).ifExists()

abstract class CoverageTest {
    protected lateinit var myDataFile: File
    abstract val coverage: Coverage
    protected open val testTracking: TestTracking? = null
    protected val commonExtraArgs = mutableListOf<String>()

    @Before
    fun setUp() {
        myDataFile = createTmpFile(".ic")
    }

    @After
    fun tearDown() {
        myDataFile.delete()
    }

    /**
     * Run coverage test with provided configuration and call verification method.
     *
     * @param testName test package name without [TEST_PACKAGE]
     * @param test source test file info, determined by [testName] by default
     * @param configuration test configuration, extracted from test source file by default
     * @param verify verification method, [verifyResults] by default
     */
    fun test(
        testName: String,
        test: TestFile = getTestFile(testName),
        configuration: TestConfiguration = extractTestConfiguration(test.file),
        verify: (ProjectData, TestConfiguration) -> Unit = this::verifyResults
    ) {
        val config = configuration.setDefaults(test)
        val project = runWithCoverage(
            myDataFile,
            testName,
            coverage,
            config.calculateUnloaded,
            testTracking,
            config.patterns!!,
            config.extraArgs,
            test.mainClass
        )
        verify(project, config)
    }

    /** Verifies coverage results. */
    open fun verifyResults(projectData: ProjectData, configuration: TestConfiguration) {
        val expectedClasses = configuration.expectedClasses
        if (expectedClasses != null) {
            val actualClasses = projectData.classesCollection.map { it.name }
            Assert.assertEquals(expectedClasses.sorted(), actualClasses.sorted())
        }
        assertEqualsLines(projectData, configuration, coverage)
    }

    /** Set default values if absent. */
    private fun TestConfiguration.setDefaults(test: TestFile): TestConfiguration {
        extraArgs.addAll(commonExtraArgs)
        val fullClassNames = when {
            classes.isEmpty() -> listOf(all)
            else -> classes.map { getFQN(test.testName, it) }
        }
        val fullExpectedClasses = when {
            expectedClasses == null -> null
            expectedClasses.any { it.startsWith(TEST_PACKAGE) } -> expectedClasses
            else -> expectedClasses.map { getFQN(test.testName, it) }
        }
        val patternsOrDefault = this.patterns ?: "$TEST_PACKAGE.${test.testName}\\..*"
        return copy(patterns = patternsOrDefault, classes = fullClassNames, expectedClasses = fullExpectedClasses)
    }
}

/**
 * Find test source file by [testName].
 * Only test.kt or Test.java files could be main class to run.
 *
 * @param testName test package name without [TEST_PACKAGE]
 */
fun getTestFile(testName: String, module: String = ""): TestFile {
    fun getFile(name: String) = pathToFile(module,"src", TEST_PACKAGE, *testName.split('.').toTypedArray(), name)
    val testKtCandidate = getFile("test.kt")
    if (testKtCandidate.isFile && testKtCandidate.exists()) {
        return TestFile(testName, testKtCandidate, getFQN(testName, "TestKt"))
    }
    val testJavaCandidate = getFile("Test.java")
    if (testJavaCandidate.isFile && testJavaCandidate.exists()) {
        return TestFile(testName, testJavaCandidate, getFQN(testName, "Test"))
    }
    error("No Test.java or test.kt file found!")
}

fun getFQN(testName: String, className: String) = "$TEST_PACKAGE.$testName.$className"
