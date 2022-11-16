/*
 * Copyright 2000-2022 JetBrains s.r.o.
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
package com.intellij.rt.coverage.offline

import com.intellij.rt.coverage.instrument.InstrumentatorTest.Companion.createInstrumentatorTask
import com.intellij.rt.coverage.instrument.InstrumentatorTest.Companion.runInstrumentator
import com.intellij.rt.coverage.report.TestUtils
import com.intellij.rt.coverage.report.XMLTest
import com.intellij.rt.coverage.report.data.BinaryReport
import com.intellij.rt.coverage.report.data.Filters
import com.intellij.rt.coverage.util.ProcessUtil
import com.intellij.rt.coverage.util.RawHitsReport
import com.intellij.rt.coverage.util.ResourceUtil
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.util.regex.Pattern

class OfflineCoverageTest {
    @Test
    fun testBranches() = test("branches")

    @Test
    fun testCrossinline() = test("crossinline")

    @Test
    fun testDefaultArgs() = test("defaultArgs")

    @Test
    fun testInline() = test("inline")

    private fun test(testName: String) {
        instrumentTestClasses()
        val report = runCoverage("testData.$testName.TestKt")
        val xmlFile = XMLTest.createXMLFile()
        TestUtils.clearLogFile(File("."))
        TestUtils.createRawReporter(report, "testData\\.$testName\\..*").createXMLReport(xmlFile)
        TestUtils.checkLogFile(File("."))
        XMLTest.verifyProjectXML(xmlFile, "xml/$testName.xml")
    }

    private fun runCoverage(className: String): BinaryReport {
        val coverageAgentPath = ResourceUtil.getAgentPath("intellij-coverage-agent")
        val classpath = roots.map { it.absolutePath } + System.getProperty("java.class.path").split(File.pathSeparator)
                .filterNot { path -> path.replace("\\\\", "/").replace("\\", "/").contains("/reporter/build/") }
                .plus(coverageAgentPath)
        val icrFile = TestUtils.createTmpFile()
        val commandLine = arrayOf(
                "-classpath", classpath.joinToString(File.pathSeparator),
                "-Dcoverage.offline.report.path=" + icrFile.absolutePath,
                className)
        TestUtils.clearLogFile(File("."))
        ProcessUtil.execJavaProcess(commandLine)
        TestUtils.checkLogFile(File("."))
        TestUtils.checkLogFile(icrFile.parentFile)
        Assert.assertTrue(RawHitsReport.isRawHitsFile(icrFile))
        return BinaryReport(icrFile, null)
    }

    private lateinit var roots: List<File>
    private fun instrumentTestClasses() {
        val (first, second) = createInstrumentatorTask()
        val filters = Filters(listOf(Pattern.compile("testData.*")), emptyList(), emptyList())
        runInstrumentator(first, second, filters)
        roots = second
    }
}