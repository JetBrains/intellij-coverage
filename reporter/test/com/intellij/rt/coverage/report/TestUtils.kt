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

import com.intellij.rt.coverage.CoverageStatusTest
import com.intellij.rt.coverage.aggregate.Aggregator
import com.intellij.rt.coverage.report.ReportLoadStrategy.AggregatedReportLoadStrategy
import com.intellij.rt.coverage.report.ReportLoadStrategy.RawReportLoadStrategy
import com.intellij.rt.coverage.report.data.BinaryReport
import com.intellij.rt.coverage.report.data.Filters
import com.intellij.rt.coverage.report.data.Module
import com.intellij.rt.coverage.report.util.FileUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern

object TestUtils {
    @JvmField
    val JAVA_OUTPUT = join("build", "classes", "java", "test")
    val KOTLIN_OUTPUT = join("build", "classes", "kotlin", "test")

    fun createTmpFile(): File {
        val directory = Files.createTempDirectory("coverage")
        return Files.createTempFile(directory, "test", ".ic").toFile()
    }

    @JvmStatic
    fun runTest(patterns: String?, className: String?): BinaryReport {
        val icFile = createTmpFile()
        val smapFile = createTmpFile()
        val classpath = System.getProperty("java.class.path").split(File.pathSeparator)
                .filter { path -> path.contains("kotlin-stdlib") }
                .plus(File(JAVA_OUTPUT).absolutePath)
                .plus(File(KOTLIN_OUTPUT).absolutePath)
                .joinToString(File.pathSeparator)
        CoverageStatusTest.runCoverage(classpath, icFile, "true ${smapFile.absolutePath} $patterns", className, true, arrayOf("-Dcoverage.ignore.private.constructor.util.class=true"), false, false)
        checkLogFile(icFile.parentFile)
        return BinaryReport(icFile, smapFile)
    }

    @JvmStatic
    fun clearLogFile(directory: File?): String? {
        val logFile = File(directory, "coverage-error.log")
        if (logFile.exists()) {
            val content = FileUtils.readAll(logFile)
            logFile.delete()
            return content
        }
        return null
    }

    @JvmStatic
    fun checkLogFile(directory: File?) {
        val content = clearLogFile(directory)
        if (!content.isNullOrEmpty()) {
            throw RuntimeException("Log file is not empty!\n$content")
        }
    }

    @JvmStatic
    fun getResourceFile(expectedFileName: String?): File {
        val expectedPath = TestUtils::class.java.classLoader.getResource(expectedFileName)!!.path
        return File(expectedPath)
    }

    @JvmStatic
    fun createRawReporter(report: BinaryReport?, patterns: String): Reporter {
        val modules = modules
        val reports = if (report == null) emptyList() else listOf(report)
        val filters = getFilters(patterns)
        return Reporter(RawReportLoadStrategy(reports, modules, filters))
    }

    @JvmStatic
    fun createReporter(report: BinaryReport, patterns: String): Reporter {
        val smapFile = File(report.dataFile.absolutePath + ".sm")
        val aggregatedReport = BinaryReport(report.dataFile, smapFile)
        runAggregator(aggregatedReport, patterns)
        val reports = listOf(aggregatedReport)
        val modules = modules
        return Reporter(AggregatedReportLoadStrategy(reports, modules))
    }

    @JvmStatic
    fun runAggregator(report: BinaryReport, patterns: String) {
        val filters = getFilters(patterns)
        val request = Aggregator.Request(filters, report.dataFile, report.sourceMapFile)
        Aggregator(listOf(report), modules, request).processRequests()
    }

    private fun getFilters(patterns: String): Filters {
        val includes: MutableList<Pattern> = ArrayList()
        val excludes: MutableList<Pattern> = ArrayList()
        val excludeAnnotations: MutableList<Pattern> = ArrayList()
        val lists = arrayOf(includes, excludes, excludeAnnotations)
        var state = 0
        for (pattern in patterns.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (pattern.isEmpty()) continue
            if (pattern == "-exclude") {
                state = 1
                continue
            }
            if (pattern == "-excludeAnnotations") {
                state = 2
                continue
            }
            lists[state].add(Pattern.compile(pattern))
        }
        return Filters(includes, excludes, excludeAnnotations)
    }

    @JvmStatic
    val modules: List<Module>
        get() {
            val output: MutableList<File> = ArrayList()
            output.add(File(KOTLIN_OUTPUT))
            output.add(File(JAVA_OUTPUT))
            return listOf(Module(output, listOf(File("test"))))
        }

    @JvmStatic
    fun join(first: String, vararg other: String): String = Paths.get(first, *other).toFile().path
}