/*
 * Copyright 2000-2023 JetBrains s.r.o.
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
import com.intellij.rt.coverage.Coverage
import com.intellij.rt.coverage.TestTracking
import com.intellij.rt.coverage.data.ProjectData
import com.intellij.rt.coverage.instrumentation.CoverageTransformer
import com.intellij.rt.coverage.instrumentation.InstrumentationUtils
import com.intellij.rt.coverage.instrumentation.testTracking.TestTrackingArrayMode
import com.intellij.rt.coverage.instrumentation.testTracking.TestTrackingClassDataMode
import com.intellij.rt.coverage.pathToFile
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

    @Test
    fun testCasesWhenString() = test("cases.whenString")

    private fun test(testName: String, simpleCLassName: String = "TestKt") {
        val className = "$TEST_PACKAGE.$testName.$simpleCLassName"
        val outputRoot = pathToFile("build", "classes", "kotlin", "test")
        val path = className.replace(".", File.separator) + ".class"
        val originalBytes = File(outputRoot, path).readBytes()

        assertFalse(originalBytes.isEmpty())

        val expectedRoot = "bytecode/${testName.replace(".", "/")}"
        assertBytecode("$expectedRoot/original.txt", originalBytes)

        val condyPossible = InstrumentationUtils.getBytecodeVersion(ClassReader(originalBytes)) >= Opcodes.V11
        for (coverage in Coverage.values()) {
            if (coverage.isCondyEnabled() && !condyPossible) {
                println("Condy test disabled due to low class file version")
                continue
            }
            doTest(null, coverage, originalBytes, className, expectedRoot)
        }
        for (testTracking in TestTracking.values()) {
            doTest(testTracking, Coverage.BRANCH, originalBytes, className, expectedRoot)
        }
    }

    private fun doTest(
        testTracking: TestTracking?,
        coverage: Coverage,
        originalBytes: ByteArray,
        className: String,
        expectedRoot: String
    ) {
        val testTrackingMode = testTracking.createMode()
        OptionsUtil.FIELD_INSTRUMENTATION_ENABLED = coverage != Coverage.LINE && coverage != Coverage.BRANCH
        OptionsUtil.CONDY_ENABLED = coverage == Coverage.CONDY_LINE || coverage == Coverage.CONDY_BRANCH
        val projectData = ProjectData(null, coverage.isBranchCoverage(), testTrackingMode?.createTestTrackingCallback())
        val transformer = CoverageTransformer(projectData, false, null, testTrackingMode)
        val bytes = transformer.instrument(originalBytes, className, null, false)

        getReadableBytecode(bytes).preprocess()
        val expectedFileName = createExpectedFileName(expectedRoot, coverage, testTracking)
        assertBytecode(expectedFileName, bytes)
    }

    private fun getExpectedBytecode(expectedFileName: String): String {
        val resource = this::class.java.classLoader.getResource(expectedFileName)
        checkNotNull(resource) { "Expected file $expectedFileName does not exist" }
        return File(resource.path).readText()
    }

    private fun assertBytecode(expectedFileName: String, actual: ByteArray) {
        val expectedBytecode = getExpectedBytecode(expectedFileName).preprocess()
        val actualBytecode = getReadableBytecode(actual).preprocess()

        Assert.assertEquals("Bytecode differs at $expectedFileName", expectedBytecode, actualBytecode)
    }
}


private fun createExpectedFileName(
    expectedRoot: String,
    coverage: Coverage,
    testTracking: TestTracking?
) = buildString {
    append(expectedRoot)
    append("/")
    val coverageName = when (coverage) {
        Coverage.LINE -> "line"
        Coverage.NEW_LINE -> "line_field"
        Coverage.BRANCH -> "branch"
        Coverage.NEW_BRANCH -> "branch_field"
        Coverage.CONDY_LINE -> "line_condy"
        Coverage.CONDY_BRANCH -> "branch_condy"
    }
    append(coverageName)
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
