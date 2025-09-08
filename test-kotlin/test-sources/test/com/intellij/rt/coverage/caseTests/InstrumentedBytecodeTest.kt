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

package com.intellij.rt.coverage.caseTests

import com.intellij.rt.coverage.*
import com.intellij.rt.coverage.data.ProjectData
import com.intellij.rt.coverage.instrumentation.InstrumentationOptions
import com.intellij.rt.coverage.instrumentation.CoverageTransformer
import com.intellij.rt.coverage.instrumentation.InstrumentationUtils
import com.intellij.rt.coverage.instrumentation.data.ProjectContext
import com.intellij.rt.coverage.instrumentation.testTracking.TestTrackingArrayMode
import com.intellij.rt.coverage.instrumentation.testTracking.TestTrackingClassDataMode
import com.intellij.rt.coverage.util.OptionsUtil
import org.jetbrains.coverage.org.objectweb.asm.ClassReader
import org.jetbrains.coverage.org.objectweb.asm.Opcodes
import org.jetbrains.coverage.org.objectweb.asm.util.TraceClassVisitor
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.assertFalse

class InstrumentedBytecodeTest {
    @Test
    fun testSimpleBranches() = test("simple.branches", "MyBranchedClass")

    private fun test(testName: String, simpleCLassName: String = "TestKt") {
        val className = "$TEST_PACKAGE.$testName.$simpleCLassName"
        val outputRoot = pathToFile("build", "classes", "kotlin", "main")
        val path = className.replace(".", File.separator) + ".class"
        val originalBytes = File(outputRoot, path).readBytes()

        assertFalse(originalBytes.isEmpty())

        val expectedRoot = "bytecode/${testName.replace(".", "/")}"
        assertBytecode("$expectedRoot/original.txt", originalBytes)

        val classVersion = InstrumentationUtils.getBytecodeVersion(ClassReader(originalBytes))
        for (coverage in Coverage.values()) {
            for (hits in listOf(true, false)) {
                val expectedCoverage = getExpectedCoverage(coverage, classVersion)
                val expectedFileName = createExpectedFileName(expectedRoot, expectedCoverage, null, hits)
                doTest(null, coverage, originalBytes, className, expectedFileName, hits)
            }
        }
        for (testTracking in TestTracking.values()) {
            val coverage = Coverage.BRANCH_FIELD
            val hits = false
            val expectedCoverage = getExpectedCoverage(coverage, classVersion)
            val expectedFileName = createExpectedFileName(expectedRoot, expectedCoverage, testTracking, hits)
            doTest(testTracking, coverage, originalBytes, className, expectedFileName, hits)
        }
    }

    private fun getExpectedCoverage(attempted: Coverage, classVersion: Int): Coverage {
        var result = attempted
        if (result.isCondyEnabled() && classVersion < Opcodes.V11) {
            result = if (result.isBranchCoverage()) Coverage.BRANCH_INDY else Coverage.LINE_INDY
        }
        if (result.isIndyEnabled() && classVersion < Opcodes.V1_7) {
            result = if (result.isBranchCoverage()) Coverage.BRANCH_FIELD else Coverage.LINE_FIELD
        }
        return result
    }

    private fun doTest(
        testTracking: TestTracking?,
        coverage: Coverage,
        originalBytes: ByteArray,
        className: String,
        expectedFileName: String,
        calculateHits: Boolean,
    ) {
        val systemProps = mutableMapOf(
            OptionsUtil::FIELD_INSTRUMENTATION_ENABLED to (coverage != Coverage.LINE && coverage != Coverage.BRANCH),
            OptionsUtil::CALCULATE_HITS_COUNT to calculateHits,
        )
        if (!coverage.isCondyEnabled()) {
            systemProps[OptionsUtil::CONDY_ENABLED] = false
            if (!coverage.isIndyEnabled()) {
                systemProps[OptionsUtil::INDY_ENABLED] = false
            }
        }
        runWithOptions(systemProps) {
            val testTrackingMode = testTracking.createMode()
            val projectData = ProjectData(testTrackingMode?.createTestTrackingCallback(null))
            val options = InstrumentationOptions.Builder()
                .setBranchCoverage(coverage.isBranchCoverage())
                .setTestTrackingMode(testTrackingMode)
                .build()
            val transformer = CoverageTransformer(projectData, ProjectContext(options))
            val bytes = transformer.instrument(originalBytes, className, null, false)

            assertBytecode(expectedFileName, bytes)
//        File("resources", expectedFileName).writeText(getReadableBytecode(bytes).preprocess())
        }
    }

    private fun getExpectedFile(expectedFileName: String): File {
        val resource = this::class.java.classLoader.getResource(expectedFileName)
        checkNotNull(resource) { "Expected file $expectedFileName does not exist" }
        return File(resource.path)
    }

    private fun assertBytecode(expectedFileName: String, actual: ByteArray) {
        val expectedBytecode = getExpectedFile(expectedFileName).readText().preprocess()
        val actualBytecode = getReadableBytecode(actual).preprocess()

        Assert.assertEquals("Bytecode differs at $expectedFileName", expectedBytecode, actualBytecode)
    }
}

private fun createExpectedFileName(
    expectedRoot: String,
    coverage: Coverage,
    testTracking: TestTracking?,
    hits: Boolean,
) = buildString {
    append(expectedRoot)
    append("/")
    val coverageName = when (coverage) {
        Coverage.LINE -> "line"
        Coverage.LINE_FIELD -> "line_field"
        Coverage.BRANCH -> "branch"
        Coverage.BRANCH_FIELD -> "branch_field"
        Coverage.LINE_INDY -> "line_indy"
        Coverage.BRANCH_INDY -> "branch_indy"
        Coverage.LINE_CONDY -> "line_condy"
        Coverage.BRANCH_CONDY -> "branch_condy"
    }
    append(coverageName)
    if (hits) append("_with_hits")
    if (testTracking != null) {
        val testTrackingName = when (testTracking) {
            TestTracking.ARRAY -> "with_test_tracking_new"
            TestTracking.CLASS_DATA -> "with_test_tracking"
        }
        append("_").append(testTrackingName)
    }
    append(".txt")
}

private fun String.preprocess() = this.toSystemIndependent().split("\n")
    .filterNot {
        it.contains("@Lkotlin/Metadata;(mv=") || it.startsWith("// class version ")
    }.joinToString("\n")

private fun getReadableBytecode(bytes: ByteArray?): String {
    val writer = StringWriter()
    val visitor = TraceClassVisitor(PrintWriter(writer))
    ClassReader(bytes).accept(visitor, 0)
    return writer.toString()
}

internal fun TestTracking?.createMode() = when (this) {
    null -> null
    TestTracking.ARRAY -> TestTrackingArrayMode()
    TestTracking.CLASS_DATA -> TestTrackingClassDataMode()
}
