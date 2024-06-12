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

package com.intellij.rt.coverage

import com.intellij.rt.coverage.data.ClassData
import com.intellij.rt.coverage.data.LineData
import com.intellij.rt.coverage.data.ProjectData
import com.intellij.rt.coverage.data.instructions.LineInstructions
import com.intellij.rt.coverage.util.CoverageRunner
import com.intellij.rt.coverage.util.TestTrackingCallback
import com.intellij.rt.coverage.util.TestTrackingIOUtil
import org.junit.Assert
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.reflect.KMutableProperty

internal enum class Coverage {
    LINE, LINE_FIELD, BRANCH, BRANCH_FIELD, LINE_CONDY, BRANCH_CONDY;

    fun isBranchCoverage() = this == BRANCH || this == BRANCH_FIELD || this == BRANCH_CONDY
    fun isCondyEnabled() = this == LINE_CONDY || this == BRANCH_CONDY

    companion object {
        fun valuesWithCondyWhenPossible() =
            if (getVMVersion() >= 11) values() else values().filterNot { it.isCondyEnabled() }.toTypedArray()
    }
}

internal enum class TestTracking {
    ARRAY, CLASS_DATA
}

internal abstract class TestResult {
    abstract val prefix: String
    abstract fun collectActualData(project: ProjectData, configuration: TestConfiguration, ): Map<Int, String>
    abstract fun provideMatcher(): Matcher<Map<Int, String>>

    open fun collectExpectedData(configuration: TestConfiguration, coverage: Coverage): Map<Int, String> {
        val fileWithMarkers = configuration.fileWithMarkers!!
        return provideMatcher().also { processFile(fileWithMarkers, it) }.result
    }

    internal object CoverageResults : TestResult() {
        override val prefix: String get() = "// coverage: "
        override fun collectActualData(
            project: ProjectData,
            configuration: TestConfiguration,
        ): Map<Int, String> {
            return coverageLines(project, configuration.classes)
                .associate { it.lineNumber to it.status.statusToString() }
        }

        override fun provideMatcher(): Matcher<Map<Int, String>> = CoverageMatcher()
        override fun collectExpectedData(configuration: TestConfiguration, coverage: Coverage): Map<Int, String> {
            val result = configuration.coverageData ?: super.collectExpectedData(configuration, coverage)
            return  if (coverage.isBranchCoverage()) result else result.remapPartialToFull()
        }

        private fun Int.statusToString() = when (this) {
            0 -> "NONE"
            1 -> "PARTIAL"
            else -> "FULL"
        }

        private fun Map<Int, String>.remapPartialToFull() = mapValues { if (it.value == "PARTIAL") "FULL" else it.value }
    }

    internal object BranchResults : TestResult() {
        override val prefix: String get() = "// branches: "
        override fun collectActualData(project: ProjectData, configuration: TestConfiguration): Map<Int, String> {
            return coverageLines(project, configuration.classes)
                .filter { it.branchData != null && it.branchData.totalBranches > 0 }
                .associate { lineData ->
                    val branches = lineData.branchData!!
                    lineData.lineNumber to "${branches.coveredBranches}/${branches.totalBranches}"
                }
        }

        override fun collectExpectedData(configuration: TestConfiguration, coverage: Coverage): Map<Int, String> {
            if (!coverage.isBranchCoverage()) return emptyMap()
            return super.collectExpectedData(configuration, coverage)
        }

        override fun provideMatcher(): Matcher<Map<Int, String>> = BranchesInfoMatcher()
    }

    protected fun coverageLines(project: ProjectData, classNames: List<String>): List<LineData> {
        val allData = ClassData("")
        getClasses(classNames, project).forEach { allData.merge(it) }
        return allData.getLinesData()
    }

    internal object InstructionResults : TestResult() {
        override val prefix: String
            get() = "// stats: "

        override fun collectActualData(project: ProjectData, configuration: TestConfiguration): Map<Int, String> {
            val classNames = configuration.classes
            val instructions = collectInstructions(project, classNames)
            return coverageLines(project, classNames)
                .associate { line ->
                    val instruction = instructions[line.lineNumber]!!.getInstructionsData(line)
                    line.lineNumber to "${instruction.coveredBranches}/${instruction.totalBranches}"
                }
        }

        override fun provideMatcher(): Matcher<Map<Int, String>> = InstructionsInfoMatcher()

        private fun collectInstructions(project: ProjectData, classNames: List<String>): Map<Int, LineInstructions> {
            val instructions = hashMapOf<Int, LineInstructions>()
            getClasses(classNames, project).forEach { classData ->
                val classInstructions = project.instructions[classData.name]!!
                classData.lines.filterIsInstance<LineData>().forEach {
                    instructions.getOrPut(it.lineNumber) { LineInstructions() }
                        .merge(classInstructions.getlines()[it.lineNumber])
                }
            }
            return instructions
        }
    }

    companion object {
        fun collectTestResults(configuration: TestConfiguration, coverage: Coverage): List<TestResult> {
            if (configuration.fileWithMarkers == null) return listOf(CoverageResults)
            if (coverage.isBranchCoverage()) return listOf(CoverageResults, BranchResults)
            return listOf(CoverageResults)
        }
    }
}

/**
 * Creates temporary file in a separate directory
 */
fun createTmpFile(suffix: String): File {
    val directory = Files.createTempDirectory("coverage")
    return Files.createTempFile(directory, "test", suffix).toFile()
}

private val classpath = System.getProperty("java.class.path").split(File.pathSeparator)
    .filter { !it.contains("intellij-coverage") || it.contains("test-kotlin") || it.contains("benchmark") }
    .joinToString(File.pathSeparator)

internal fun runWithCoverage(
    coverageDataFile: File,
    testName: String,
    coverage: Coverage,
    calcUnloaded: Boolean = false,
    testTracking: TestTracking? = null,
    patterns: String = "$TEST_PACKAGE.*",
    extraArgs: MutableList<String> = mutableListOf(),
    mainClass: String = getTestFile(testName).mainClass
): ProjectData {
    when (coverage) {
        Coverage.LINE_FIELD, Coverage.BRANCH_FIELD -> extraArgs.add("-Dcoverage.condy.enable=false")
        Coverage.LINE, Coverage.BRANCH -> extraArgs.add("-Didea.new.tracing.coverage=false")
        else -> {}
    }
    if (testTracking == TestTracking.CLASS_DATA) {
        extraArgs.add("-Didea.new.test.tracking.coverage=false")
    }
    return CoverageRunner.runCoverage(
        classpath, coverageDataFile, patterns, mainClass,
        coverage.isBranchCoverage(), extraArgs.toTypedArray(), calcUnloaded, testTracking != null
    )
        .also { assertEmptyLogFile(coverageDataFile) }
}

internal inline fun runWithOptions(values: Map<KMutableProperty<Boolean>, Boolean>, action: () -> Unit) {
    val originalValues = values.mapValues { it.key.getter.call() }
    values.forEach { (field, value) -> field.setter.call(value) }
    try {
        action()
    } finally {
        originalValues.forEach { (field, value) -> field.setter.call(value) }
    }
}

internal fun assertEmptyLogFile(coverageDataFile: File) {
    logFile(coverageDataFile)?.readText()?.also { log -> throw RuntimeException(log) }
}

internal fun assertEqualsLines(project: ProjectData, configuration: TestConfiguration, coverage: Coverage) {
    val testResults = TestResult.collectTestResults(configuration, coverage)
    val fileWithMarkers = configuration.fileWithMarkers
    if (fileWithMarkers != null) {
        for (testResult in testResults) {
            assertEqualsFiles(fileWithMarkers, testResult, project, configuration, coverage)
        }
    } else {
        for (testResult in testResults) {
            val expected = testResult.collectExpectedData(configuration, coverage)
            val actual = testResult.collectActualData(project, configuration)
            Assert.assertEquals(expected, actual)
        }
    }
}

internal fun assertEqualsTestTracking(
    coverageDataFile: File,
    expected: Map<Int, Set<String>>,
    classNames: List<String>
) {
    val actual = testTrackingLines(coverageDataFile, classNames)
    Assert.assertEquals(expected, actual)
}

internal fun assertEqualsFiles(file: File, testResult: TestResult, project: ProjectData, configuration: TestConfiguration, coverage: Coverage) {
    val regex = testResult.provideMatcher().regex
    val prefix = testResult.prefix
    val actual = testResult.collectActualData(project, configuration)
    val expected = testResult.collectExpectedData(configuration, coverage)
    val originalContent = file.readText()
    val withoutComments = originalContent.lines().map { line ->
        if (!line.contains(regex)) line to -1 else {
            val range = regex.find(line)!!.range
            line.removeRange(range) to range.first
        }
    }
    val withActualComments = replaceData(withoutComments, actual, prefix)
    val withExpectedComments = replaceData(withoutComments, expected, prefix)
    Assert.assertEquals(withExpectedComments, withActualComments)
}

private fun replaceData(
    withoutComments: List<Pair<String, Int>>,
    data: Map<Int, String>,
    prefix: String
): String {
    val withActualComments = withoutComments.mapIndexed { i, (line, offset) ->
        val mark = data[i + 1]
        if (mark == null) line else if (offset == -1) "$line $prefix$mark" else
            "${line.substring(0, offset)}$prefix$mark${line.substring(offset)}"
    }.joinToString("\n")
    return withActualComments
}

internal const val all = "ALL CLASSES"
private fun getClasses(classNames: List<String>, project: ProjectData) = if (classNames.contains(all)) {
    project.classes.values.filter { it.name.startsWith(TEST_PACKAGE) }
} else {
    classNames.map { name ->
        project.getClassData(name)
            .also { checkNotNull(it) { "Class $name has not been found in the coverage report!" } }
    }
}

internal fun testTrackingLines(coverageDataFile: File, classNames: List<String>): Map<Int, Set<String>> {
    val result = hashMapOf<Int, MutableSet<String>>()
    val data = loadTestTrackingData(coverageDataFile)
    for ((testName, testData) in data) {
        for ((className, coveredLines) in testData) {
            if (all in classNames || className in classNames) {
                for (line in coveredLines) {
                    result.computeIfAbsent(line) { hashSetOf() }.add(testName)
                }
            }
        }
    }
    return result
}

private fun loadTestTrackingData(coverageDataFile: File): Map<String, Map<String, IntArray>> {
    val tracesDir = TestTrackingCallback.createTracesDir(coverageDataFile)
    return try {
        TestTrackingIOUtil.loadTestTrackingData(tracesDir)
    } finally {
        tracesDir.deleteRecursively()
    }
}

private fun ClassData.getLinesData() = (lines ?: Array<LineData?>(0) { null })
    .filterIsInstance<LineData>().sortedBy { it.lineNumber }

internal fun extractTestTrackingDataFromFile(file: File): Map<Int, Set<String>> =
    extractInfoFromFile(file, TestTrackingMatcher())

internal fun extractInstructionsInfoFromFile(file: File): Map<Int, String> =
    extractInfoFromFile(file, InstructionsInfoMatcher())

internal fun extractBranchesInfoFromFile(file: File): Map<Int, String> =
    extractInfoFromFile(file, BranchesInfoMatcher())

private fun <T> extractInfoFromFile(file: File, matcher: Matcher<T>): T {
    processFile(file, matcher)
    return matcher.result
}

internal fun pathToFile(name: String, vararg names: String): File = Paths.get(name, *names).toFile()
internal fun String.toSystemIndependent() = replace("\r\n", "\n")
internal fun <T, U> Iterable<T>.product(other: Iterable<U>): List<Pair<T, U>> =
    flatMap { l -> other.map { r -> l to r } }

private fun getVMVersion(): Int {
    var version = System.getProperty("java.version")
    if (version.startsWith("1.")) {
        version = version.substring(2, 3)
    } else {
        val dot = version.indexOf(".")
        if (dot != -1) {
            version = version.substring(0, dot)
        }
    }
    return version.toInt()
}

internal fun extractTestConfiguration(file: File): TestConfiguration {
    val classes = StringListMatcher(classesMarkerRegex, 1)
    val expectedClasses = StringListMatcher(expectedClassesMarkerRegex, 1)
    val extraArgs = StringListMatcher(extraArgumentsMarkerRegex, 1)
    val patterns = StringMatcher(patternsMarkerRegex, 1)
    val calculateUnloaded = FlagMatcher(calculateUnloadedMarkerRegex, 1)
    val otherFile = StringMatcher(fileWithCoverageMarkersRegex, 1)

    processFile(file, classes, expectedClasses, extraArgs, patterns, calculateUnloaded, otherFile)
    val otherFileName = otherFile.result
    val fileWithMarkers = if (otherFileName != null) File(file.parentFile, otherFileName) else file
    return TestConfiguration(
        classes.result ?: emptyList(),
        patterns.result,
        extraArgs.result ?: mutableListOf(),
        calculateUnloaded.result,
        expectedClasses.result,
        fileWithMarkers,
    )
}

internal fun processFile(file: File, vararg callbacks: Matcher<*>) {
    file.useLines {
        it.forEachIndexed { line, s ->
            for (callback in callbacks) {
                callback.match(line + 1, s)
            }
        }
    }
}

private val coverageMarkerRegex = Regex("// coverage: (FULL|PARTIAL|NONE)(?= |$)")
private val classesMarkerRegex = Regex("// classes: (.*)\$")
private val expectedClassesMarkerRegex = Regex("// class: (.*)\$")
private val patternsMarkerRegex = Regex("// patterns: (.*)\$")
private val calculateUnloadedMarkerRegex = Regex("// calculate unloaded: (.*)\$")
private val extraArgumentsMarkerRegex = Regex("// extra args: (.*)\$")
private val testTrackingMarkerRegex = Regex("// tests: ([^/]*)")
private val fileWithCoverageMarkersRegex = Regex("// markers: (.*)\$")
private val instructionsInfoRegex = Regex("// stats: ([0-9]+)/([0-9]+)(?= |$)")
private val branchesInfoRegex = Regex("// branches: ([0-9]+)/([0-9]+)(?= |$)")

/**
 * Collect state from found matches in the lines of a file.
 */
internal abstract class Matcher<T>(val regex: Regex) {
    abstract val result: T
    abstract fun onMatchFound(line: Int, match: MatchResult)
    fun match(line: Int, s: String) {
        val match = regex.find(s)
        if (match != null) {
            onMatchFound(line, match)
        }
    }
}

internal abstract class SingleGroupMatcher<T>(regex: Regex, private val group: Int) : Matcher<T>(regex) {
    abstract fun onMatchFound(line: Int, match: String)
    override fun onMatchFound(line: Int, match: MatchResult) = onMatchFound(line, match.groupValues[group])
}

private class StringMatcher(regex: Regex, group: Int) : SingleGroupMatcher<String?>(regex, group) {
    private var value: String? = null
    override val result get() = value

    override fun onMatchFound(line: Int, match: String) {
        value = match
    }
}

private class StringListMatcher(regex: Regex, group: Int) : SingleGroupMatcher<List<String>?>(regex, group) {
    private var values: MutableList<String>? = null
    override val result get() = values
    override fun onMatchFound(line: Int, match: String) {
        var result = values
        if (result == null) {
            result = mutableListOf()
            values = result
        }
        result.addAll(match.split(' '))
    }
}

private class FlagMatcher(regex: Regex, group: Int = 1) : SingleGroupMatcher<Boolean>(regex, group) {
    private var value = false
    override val result get() = value

    override fun onMatchFound(line: Int, match: String) {
        check(match == "true" || match == "false") { "Boolean value expected: $match" }
        value = match == "true"
    }
}

private class TestTrackingMatcher : SingleGroupMatcher<Map<Int, Set<String>>>(testTrackingMarkerRegex, 1) {
    override val result = linkedMapOf<Int, Set<String>>()
    override fun onMatchFound(line: Int, match: String) {
        this.result[line] = match.trim().split(' ').toSet()
    }
}

private class CoverageMatcher : SingleGroupMatcher<Map<Int, String>>(coverageMarkerRegex, 1) {
    override val result = linkedMapOf<Int, String>()
    override fun onMatchFound(line: Int, match: String) {
        result[line] = match
    }
}

private class InstructionsInfoMatcher : Matcher<Map<Int, String>>(instructionsInfoRegex) {
    override val result = linkedMapOf<Int, String>()
    override fun onMatchFound(line: Int, match: MatchResult) {
        val coveredInstructions = match.groupValues[1]
        val totalInstructions = match.groupValues[2]
        result[line] = "$coveredInstructions/$totalInstructions"
    }
}

private class BranchesInfoMatcher : Matcher<Map<Int, String>>(branchesInfoRegex) {
    override val result = linkedMapOf<Int, String>()
    override fun onMatchFound(line: Int, match: MatchResult) {
        val covered = match.groupValues[1]
        val total = match.groupValues[2]
        result[line] = "$covered/$total"
    }
}

internal open class DirectiveMatcher(directive: String) : Matcher<Boolean>(Regex(directive)) {
    private var matchFound = false
    override val result get() = matchFound
    override fun onMatchFound(line: Int, match: MatchResult) {
        matchFound = true
    }
}

internal class IncludeInstructionsMatcher : DirectiveMatcher("// instructions & branches")
