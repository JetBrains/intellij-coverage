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
package com.intellij.rt.coverage.report

import com.intellij.rt.coverage.data.LineCoverage
import com.intellij.rt.coverage.data.LineData
import com.intellij.rt.coverage.data.ProjectData
import com.intellij.rt.coverage.data.instructions.ClassInstructions
import com.intellij.rt.coverage.data.instructions.LineInstructions
import com.intellij.rt.coverage.report.TestUtils.checkLogFile
import com.intellij.rt.coverage.report.TestUtils.clearLogFile
import com.intellij.rt.coverage.report.TestUtils.createFilters
import com.intellij.rt.coverage.report.TestUtils.createRawReporter
import com.intellij.rt.coverage.report.TestUtils.createReporter
import com.intellij.rt.coverage.report.TestUtils.getResourceFile
import com.intellij.rt.coverage.report.TestUtils.runTest
import com.intellij.rt.coverage.report.api.ReportApi
import com.intellij.rt.coverage.report.util.FileUtils
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.regex.Pattern

class XMLTest {
    @Test
    fun testBranches() {
        test("branches")
    }

    @Test
    fun testCrossinline() {
        test("crossinline")
    }

    @Test
    fun testDefaultArgs() {
        test("defaultArgs")
    }

    @Test
    fun testDeprecated() {
        test("deprecated")
    }

    @Test
    fun testEmptyMethod() {
        test("emptyMethod")
    }

    @Test
    fun testExcludeAnnotation() {
        val patterns = "testData.excludeAnnotation.* -excludeAnnotations testData.excludeAnnotation.ExcludeFromCoverage"
        val className = "testData.excludeAnnotation.TestKt"
        verifyXML(patterns, className, "xml/excludeAnnotation.xml")
    }

    @Test
    fun testIncludeAnnotation() {
        val patterns = "testData.includeAnnotation.* -includeAnnotations testData.includeAnnotation.IncludeCoverage"
        val className = "testData.includeAnnotation.TestKt"
        verifyXML(patterns, className, "xml/includeAnnotation.xml")
    }

    @Test
    fun testInline() {
        test("inline")
    }

    @Test
    fun testNoReport() {
        val xmlFile = createXMLFile()
        createRawReporter(null, "testData.noReport.*").createXMLReport(xmlFile)
        verifyXMLWithExpected(xmlFile, "xml/noReport.xml")
    }

    @Test
    fun testSimple() {
        val patterns = "testData.simple.*"
        val className = "testData.simple.Main"
        verifyXML(patterns, className, "xml/simple.xml")
    }

    @Test
    fun testProperties() {
        test("properties")
    }

    @Test
    fun apiTest() {
        val report = runTest("testData.simple.*", "testData.simple.Main")
        val xmlFile = createXMLFile()

        clearLogFile(File("."))
        val filters = createFilters(Pattern.compile("testData.simple.*"))
        ReportApi.xmlReport(
            xmlFile,
            null,
            listOf(report.dataFile),
            listOf(File(TestUtils.JAVA_OUTPUT)),
            listOf(File("test")),
            filters
        )

        checkLogFile(File("."))
        verifyXMLWithExpected(xmlFile, "xml/simple.xml")
    }

    @Test
    fun basicTest() {
        val project = ProjectData()
        val classData1 = project.getOrCreateClassData("MyClass")
        val classData2 = project.getOrCreateClassData("package.MyClass2")
        val lines1 = arrayOf(
            LineData(1, "foo(I)V"),
            LineData(2, "foo(I)V"),
            LineData(3, "boo()V")
        )

        val lines2 = arrayOf(
            LineData(1, "a(II)I"),
            LineData(2, "b(J)Z")
        )
        classData1.setLines(lines1)
        classData2.setLines(lines2)

        classData1.source = "F.java"
        classData2.source = "A.java"

        val file = createXMLFile()
        clearLogFile(File("."))
        XMLCoverageReport().write(FileOutputStream(file), project, "TITLE")
        checkLogFile(File("."))
        verifyXMLWithExpected(file, "xml/xmlTest.xml")
    }

    @Test
    fun sameFileNameTest() {
        val project = ProjectData()
        project.setInstructionsCoverage(true)
        val projectInstructions = project.instructions

        val classData1 = project.getOrCreateClassData("package.A")
        val lineData1 = LineData(1, "foo()V")
        lineData1.hits = 1
        val lines1 = arrayOf(null, lineData1)
        classData1.setLines(lines1)
        classData1.source = "A.kt"

        val lineInstructions1 = LineInstructions()
        lineInstructions1.instructions = 1
        projectInstructions["package.A"] = ClassInstructions(arrayOf(null, lineInstructions1))

        val classData2 = project.getOrCreateClassData("package.B")
        val lineData2 = LineData(1, "foo()V")
        lineData2.hits = 1
        val lines2 = arrayOf(null, lineData2)
        classData2.setLines(lines2)
        classData2.source = "A.kt"

        val lineInstructions2 = LineInstructions()
        lineInstructions2.instructions = 1
        projectInstructions["package.B"] = ClassInstructions(arrayOf(null, lineInstructions2))

        val file = createXMLFile()
        clearLogFile(File("."))
        XMLCoverageReport().write(FileOutputStream(file), project, null)
        checkLogFile(File("."))
        verifyXMLWithExpected(file, "xml/sameSource.xml")
    }

    @Test
    fun testXMLRead() {
        val inputStream = TestUtils::class.java.classLoader.getResourceAsStream("xml/simple.xml")
        val report = XMLCoverageReport().read(inputStream)

        Assert.assertEquals(1, report.classes.size)
        val classInfo = report.getClass("testData.simple.Main")
        Assert.assertNotNull(classInfo)
        Assert.assertEquals(21, classInfo.missedInstructions)
        Assert.assertEquals(16, classInfo.coveredInstructions)
        Assert.assertEquals(3, classInfo.missedBranches)
        Assert.assertEquals(2, classInfo.coveredBranches)
        Assert.assertEquals(4, classInfo.missedLines)
        Assert.assertEquals(5, classInfo.coveredLines)
        Assert.assertEquals(1, classInfo.missedMethods)
        Assert.assertEquals(1, classInfo.coveredMethods)

        Assert.assertEquals(1, report.files.size)
        val fileInfo = report.getFile("testData/simple/Main.java")
        Assert.assertNotNull(fileInfo)
        Assert.assertEquals(9, fileInfo.lines.size)
        val lineInfo = fileInfo.lines[0]
        Assert.assertEquals(19, lineInfo.lineNumber)
        Assert.assertEquals(2, lineInfo.missedInstructions)
        Assert.assertEquals(0, lineInfo.coveredInstructions)
        Assert.assertEquals(0, lineInfo.missedBranches)
        Assert.assertEquals(0, lineInfo.coveredBranches)
    }

    @Test
    fun testInherits() {
        val testName = "lambdas"
        val patterns = "testData\\.$testName\\..*"
        val report = runTest(patterns, "testData.$testName.TestKt")

        val filters = createFilters(
            includes = listOf(Pattern.compile(patterns)),
            includeInherits = listOf(Pattern.compile("kotlin\\.jvm\\.internal\\.Lambda"))
        )
        val xmlFile = createXMLFile()
        val strategy = ReportLoadStrategy.RawReportLoadStrategy(listOf(report), TestUtils.outputRoots, null, filters)
        Reporter(strategy, null).createXMLReport(xmlFile)
        verifyXMLWithExpected(xmlFile, "xml/inherits.xml")
    }

    @Test
    fun testClassAndInheritanceFilter() {
        val testName = "classAndInheritanceFilter"
        val patterns = ".*\\..*Child"
        val report = runTest(patterns, "testData.$testName.TestKt")

        val filters = createFilters(
            includes = listOf(Pattern.compile(patterns)),
            includeInherits = listOf(Pattern.compile("testData\\.classAndInheritanceFilter\\.A"))
        )
        val xmlFile = createXMLFile()
        val strategy = ReportLoadStrategy.RawReportLoadStrategy(listOf(report), TestUtils.outputRoots, null, filters)
        Reporter(strategy, null).createXMLReport(xmlFile)
        verifyXMLWithExpected(xmlFile, "xml/classAndInheritanceFilter.xml")
    }

    private fun test(testName: String) {
        val patterns = "testData\\.$testName\\..*"
        val className = "testData.$testName.TestKt"
        val expectedFileName = "xml/$testName.xml"

        verifyXML(patterns, className, expectedFileName)
    }

    companion object {
        private fun verifyXMLRead(xmlReport: File, expected: ProjectData) {
            Assert.assertTrue(XMLCoverageReport.canReadFile(xmlReport))
            val actual = XMLCoverageReport().read(FileInputStream(xmlReport))
            var classCount = 0
            val files: MutableMap<String, MutableMap<Int, XMLProjectData.LineInfo>> = HashMap()
            for (classData in expected.classesCollection) {
                if (!hasLines(classData.lines)) continue
                classCount++
                val classInfo = actual.getClass(classData.name)
                Assert.assertNotNull(classInfo)
                val methods: MutableMap<String, Boolean> = HashMap()

                Assert.assertNotNull(classData.source)
                Assert.assertEquals(classData.source, classInfo.fileName)
                val index = classData.name.lastIndexOf('.')
                val packageName = if (index < 0) "" else classData.name.substring(0, index)
                val path = if (packageName.isEmpty()) classData.source
                else packageName.replace('.', '/') + "/" + classData.source

                var fileLines = files[path]
                if (fileLines == null) {
                    fileLines = HashMap()
                    files[path] = fileLines
                }
                var mi = 0
                var ci = 0
                var mb = 0
                var cb = 0
                var mm = 0
                var cm = 0
                var ml = 0
                var cl = 0
                for (line in classData.lines as Array<LineData?>) {
                    if (line == null) continue

                    var lineInfo = fileLines[line.lineNumber]
                    if (lineInfo == null) {
                        lineInfo = XMLProjectData.LineInfo(line.lineNumber)
                        fileLines[line.lineNumber] = lineInfo
                    }
                    val branchData = line.branchData
                    if (branchData != null) {
                        lineInfo.coveredBranches += branchData.coveredBranches
                        lineInfo.missedBranches += branchData.totalBranches - branchData.coveredBranches
                    }

                    val i = expected.instructions[classData.name]!!.getlines()[line.lineNumber].instructions
                    if (line.status == LineCoverage.NONE.toInt()) {
                        ml++
                        lineInfo.missedInstructions += i
                        if (!methods.containsKey(line.methodSignature)) {
                            methods[line.methodSignature] = false
                        }
                    } else {
                        cl++
                        lineInfo.coveredInstructions += i
                        methods[line.methodSignature] = true
                    }
                    ci += lineInfo.coveredInstructions
                    mi += lineInfo.missedInstructions
                    cb += lineInfo.coveredBranches
                    mb += lineInfo.missedBranches
                }
                for ((_, value) in methods) {
                    if (value) {
                        cm++
                    } else {
                        mm++
                    }
                }
                Assert.assertEquals(mi, classInfo.missedInstructions)
                Assert.assertEquals(ci, classInfo.coveredInstructions)
                Assert.assertEquals(mb, classInfo.missedBranches)
                Assert.assertEquals(cb, classInfo.coveredBranches)
                Assert.assertEquals(ml, classInfo.missedLines)
                Assert.assertEquals(cl, classInfo.coveredLines)
                Assert.assertEquals(mm, classInfo.missedMethods)
                Assert.assertEquals(cm, classInfo.coveredMethods)
            }
            Assert.assertEquals(classCount, actual.classes.size)
            Assert.assertEquals(files.size, actual.files.size)
            for (fileInfo in actual.files) {
                val expectedFile: Map<Int, XMLProjectData.LineInfo> = files[fileInfo.path]!!
                Assert.assertNotNull(expectedFile)
                for (lineInfo in fileInfo.lines) {
                    val expectedLine = expectedFile[lineInfo.lineNumber]
                    Assert.assertNotNull(expectedLine)
                    Assert.assertEquals(expectedLine!!.coveredBranches, lineInfo.coveredBranches)
                    Assert.assertEquals(expectedLine.missedBranches, lineInfo.missedBranches)
                    Assert.assertEquals(expectedLine.coveredInstructions, lineInfo.coveredInstructions)
                    Assert.assertEquals(expectedLine.missedInstructions, lineInfo.missedInstructions)
                }
            }
        }

        private fun hasLines(lines: Array<Any?>?) = lines?.any { it != null } ?: false

        private fun runXMLTest(patterns: String, className: String): Pair<File, ProjectData> {
            val report = runTest(patterns, className)
            val xmlFile = createXMLFile()
            clearLogFile(File("."))
            val reporter = createReporter(report, patterns)
            reporter.createXMLReport(xmlFile)
            checkLogFile(File("."))
            return Pair(xmlFile, reporter.projectData)
        }

        fun createXMLFile(): File {
            return File.createTempFile("report_tmp", ".xml")
        }

        fun verifyXMLWithExpected(file: File?, expectedFileName: String?) {
            val expected = getResourceFile(expectedFileName)
            Assert.assertEquals(FileUtils.readAll(expected), FileUtils.readAll(file))
        }

        private fun verifyXML(patterns: String, className: String, expectedFileName: String) {
            val result = runXMLTest(patterns, className)

            verifyXMLWithExpected(result.first, expectedFileName)
            verifyXMLRead(result.first, result.second)
        }
    }
}
