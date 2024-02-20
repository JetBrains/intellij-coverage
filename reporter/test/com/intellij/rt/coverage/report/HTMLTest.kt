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

import com.intellij.rt.coverage.report.TestUtils.checkLogFile
import com.intellij.rt.coverage.report.TestUtils.clearLogFile
import com.intellij.rt.coverage.report.TestUtils.createFilters
import com.intellij.rt.coverage.report.TestUtils.createRawReporter
import com.intellij.rt.coverage.report.TestUtils.join
import com.intellij.rt.coverage.report.TestUtils.runTest
import com.intellij.rt.coverage.report.api.ReportApi
import com.intellij.rt.coverage.report.util.FileUtils
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.util.regex.Pattern

class HTMLTest {
    @Test
    fun testSimple() {
        verifyHTMLDir(runTestAndConvertToHTML(".*", "testData.simple.Main"))
    }

    @Test
    fun testInline() {
        verifyHTMLDir(runTestAndConvertToHTML("testData\\.inline\\..*", "testData.inline.TestKt"))
    }

    @Test
    fun testFileOutOfPackageStructure() {
        val htmlDir = runTestAndConvertToHTML(
            "testData.outOfPackageStructure\\..*",
            "testData.outOfPackageStructure.TestOutOfPackageStructureKt"
        )
        verifyHTMLDir(htmlDir)
        val sourcesFile = File(htmlDir, join("ns-1", "sources", "source-1.html"))
        Assert.assertTrue(sourcesFile.exists())
        Assert.assertFalse(FileUtils.readAll(sourcesFile).contains("Source code is not available"))
        Assert.assertTrue(FileUtils.readAll(sourcesFile).contains("package testData.outOfPackageStructure"))
    }

    @Test
    fun testTopLevel() {
        val htmlDir = runTestAndConvertToHTML("-exclude testData.*", "TestTopLevelKt")
        verifyHTMLDir(htmlDir)
        val sourcesFile = File(htmlDir, join("ns-1", "sources", "source-1.html"))
        Assert.assertTrue(sourcesFile.exists())
        Assert.assertFalse(FileUtils.readAll(sourcesFile).contains("Source code is not available"))
        Assert.assertTrue(FileUtils.readAll(sourcesFile).contains("fun main() {"))
    }

    @Test
    fun apiTest() {
        val report = runTest("testData.simple.*", "testData.simple.Main")
        val htmlDir = createHtmlDir(report.dataFile)

        val filters = createFilters(Pattern.compile("testData.simple.*"))
        ReportApi.htmlReport(
            htmlDir, DEFAULT_TITLE, null, listOf(report.dataFile),
            listOf(File(TestUtils.JAVA_OUTPUT)),
            listOf(File("test")), filters
        )

        verifyHTMLDir(htmlDir)
    }
}

private fun runTestAndConvertToHTML(patterns: String, className: String): File {
    val report = runTest(patterns, className)
    val htmlDir = createHtmlDir(report.dataFile)
    clearLogFile(File("."))
    createRawReporter(report, patterns, DEFAULT_TITLE).createHTMLReport(htmlDir, DEFAULT_CHARSET)
    checkLogFile(File("."))
    return htmlDir
}

private const val DEFAULT_TITLE = "TITLE"
private const val DEFAULT_CHARSET = "UTF-8"

private fun verifyHTMLDir(htmlDir: File) {
    Assert.assertTrue(htmlDir.exists())
    Assert.assertTrue(htmlDir.isDirectory)
    val children = htmlDir.listFiles()!!
    Assert.assertTrue(children.isNotEmpty())
    val indexFile = File(htmlDir, "index.html")
    Assert.assertTrue(indexFile.exists())
    val content = FileUtils.readAll(indexFile)
    Assert.assertTrue(content.contains("<title>$DEFAULT_TITLE Coverage Report > Summary</title>"))
    Assert.assertTrue(content.contains("<h1>$DEFAULT_TITLE: Overall Coverage Summary </h1>"))
    Assert.assertTrue(content.contains("Current scope: $DEFAULT_TITLE<span class=\"separator\">|</span>    all classes"))

    Assert.assertTrue(File(htmlDir, "ns-1").exists())
}

private fun createHtmlDir(icFile: File): File {
    val dirName = icFile.name.replace(".ic", "html")
    val htmlDir = File(icFile.parentFile, dirName)
    htmlDir.mkdir()
    return htmlDir
}
