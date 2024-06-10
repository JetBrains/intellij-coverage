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

internal fun runWithCoverage(coverageDataFile: File, testName: String, coverage: Coverage, calcUnloaded: Boolean = false, testTracking: TestTracking? = null,
                    patterns: String = "$TEST_PACKAGE.*", extraArgs: MutableList<String> = mutableListOf(),
                    mainClass: String = getTestFile(testName).mainClass): ProjectData {
    when (coverage) {
        Coverage.LINE_FIELD, Coverage.BRANCH_FIELD -> extraArgs.add("-Dcoverage.condy.enable=false")
        Coverage.LINE, Coverage.BRANCH -> extraArgs.add("-Didea.new.tracing.coverage=false")

        else -> {}
    }
    if (testTracking == TestTracking.CLASS_DATA) {
        extraArgs.add("-Didea.new.test.tracking.coverage=false")
    }
    return CoverageRunner.runCoverage(classpath, coverageDataFile, patterns, mainClass,
            coverage.isBranchCoverage(), extraArgs.toTypedArray(), calcUnloaded, testTracking != null)
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
    val actualCoverage = coverageLines(project, configuration.classes)
        .associate { it.lineNumber to it.status.statusToString() }
        .run {
            if (coverage.isBranchCoverage()) this else remapFullToPartialWhenNeeded(configuration.coverageData)
        }
    if (configuration.fileWithMarkers != null) {
        assertEqualsFiles(configuration.fileWithMarkers, actualCoverage, "// coverage: ", coverageMarkerRegex)
    }
    Assert.assertEquals(configuration.coverageData, actualCoverage)
}

private fun Map<Int, String>.remapFullToPartialWhenNeeded(expected: Map<Int, String>) =
    mapValues { if (it.value == "FULL" && expected[it.key] == "PARTIAL") "PARTIAL" else it.value }

internal fun assertEqualsTestTracking(coverageDataFile: File, expected: Map<Int, Set<String>>, classNames: List<String>) {
    val actual = testTrackingLines(coverageDataFile, classNames)
    Assert.assertEquals(expected, actual)
}

internal fun assertEqualsExtendedInfo(project: ProjectData, configuration: TestConfiguration) {
    val actual: Map<Int, String> = extendedLineInfo(project, configuration.classes)
    if (configuration.fileWithMarkers != null) {
        assertEqualsFiles(configuration.fileWithMarkers, actual, "// stats: ", extendedInfoRegex)
    }
    Assert.assertEquals(configuration.coverageData.map { it.toString() }.joinToString("\n"), actual.map { it.toString() }.joinToString("\n"))
}

private fun assertEqualsFiles(file: File, actual: Map<Int, String>, prefix: String, regex: Regex) {
    val originalContent = file.readText()
    val withoutComments = originalContent.lines().map { line ->
        if (!line.contains(regex)) line to -1 else {
            val range = regex.find(line)!!.range
            line.removeRange(range) to range.first
        }
    }
    val withActualComments = withoutComments.mapIndexed { i, (line, offset) ->
        val coverage = actual[i + 1]
        if (coverage == null) line else if (offset == -1) "$line $prefix$coverage" else
            "${line.substring(0, offset)}$prefix$coverage${line.substring(offset)}"
    }.joinToString("\n")
    Assert.assertEquals(originalContent, withActualComments)
}

internal const val all = "ALL CLASSES"

private fun coverageLines(project: ProjectData, classNames: List<String>): List<LineData> {
    val allData = ClassData("")
    getClasses(classNames, project).forEach { allData.merge(it) }
    return allData.getLinesData()
}

private fun extendedLineInfo(project: ProjectData, classNames: List<String>): Map<Int, String> = coverageLines(project, classNames)
        .run {
            val instructionsMap = hashMapOf<Int, LineInstructions>()
            getClasses(classNames, project).forEach { classData ->
                val classInstructions = project.instructions[classData.name]!!
                classData.lines.filterIsInstance<LineData>().forEach { instructionsMap.getOrPut(it.lineNumber) { LineInstructions() }.merge(classInstructions.getlines()[it.lineNumber]) }
            }
            associate { line ->
                val branches = line.branchData
                val instructions = instructionsMap[line.lineNumber]!!.getInstructionsData(line)
                val branchCoverage = if (branches != null) " ${branches.coveredBranches}/${branches.totalBranches}" else ""
                line.lineNumber to "${instructions.coveredBranches}/${instructions.totalBranches}$branchCoverage"
            }
        }

private fun getClasses(classNames: List<String>, project: ProjectData) = if (classNames.contains(all)) {
    project.classes.values.filter { it.name.startsWith(TEST_PACKAGE) }
} else {
    classNames.map { name -> project.getClassData(name)
        .also { checkNotNull(it) {"Class $name has not been found in the coverage report!"} } }
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

private fun Int.statusToString() = when (this) {
    0 -> "NONE"
    1 -> "PARTIAL"
    else -> "FULL"
}

private fun ClassData.getLinesData() = (lines ?: Array<LineData?>(0) { null })
        .filterIsInstance<LineData>().sortedBy { it.lineNumber }

internal fun extractTestTrackingDataFromFile(file: File): Map<Int, Set<String>> = TestTrackingMatcher().let { callback ->
        processFile(file, callback)
        callback.result
    }

internal fun extractExtendedInfoFromFile(file: File): Map<Int, String> = ExtendedInfoMatcher().let { matcher ->
    processFile(file, matcher)
    matcher.result
}

internal fun pathToFile(name: String, vararg names: String): File = Paths.get(name, *names).toFile()
internal fun String.toSystemIndependent() = replace("\r\n", "\n")
internal fun <T, U> Iterable<T>.product(other: Iterable<U>): List<Pair<T, U>> = flatMap { l -> other.map { r -> l to r } }
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
    val coverage = CoverageMatcher()
    val classes = StringListMatcher(classesMarkerRegex, 1)
    val expectedClasses = StringListMatcher(expectedClassesMarkerRegex, 1)
    val extraArgs = StringListMatcher(extraArgumentsMarkerRegex, 1)
    val patterns = StringMatcher(patternsMarkerRegex, 1)
    val calculateUnloaded = FlagMatcher(calculateUnloadedMarkerRegex, 1)
    val otherFile = StringMatcher(fileWithCoverageMarkersRegex, 1)

    var fileWithMarkers = file
    processFile(file, coverage, classes, expectedClasses, extraArgs, patterns, calculateUnloaded, otherFile)
    val otherFileName = otherFile.value
    if (otherFileName !== null) {
        coverage.result.clear()
        fileWithMarkers = File(file.parentFile, otherFileName)
        processFile(fileWithMarkers, coverage)
    }
    return TestConfiguration(
        coverage.result,
        classes.values ?: emptyList(),
        patterns.value,
        extraArgs.values ?: mutableListOf(),
        calculateUnloaded.value,
        expectedClasses.values,
        fileWithMarkers,
    )
}

internal fun processFile(file: File, vararg callbacks: Matcher) {
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
private val extendedInfoRegex = Regex("// stats: ([0-9]+)/([0-9]+)(\\s+([0-9]+)/([0-9]+))?(?= |$)")

/**
 * Collect state from found matches in the lines of a file.
 */
abstract class Matcher(private val regex: Regex) {
    abstract fun onMatchFound(line: Int, match: MatchResult)
    fun match(line: Int, s: String) {
        val match = regex.find(s)
        if (match != null) {
            onMatchFound(line, match)
        }
    }
}

abstract class SingleGroupMatcher(regex: Regex, private val group: Int) : Matcher(regex) {
    abstract fun onMatchFound(line: Int, match: String)
    override fun onMatchFound(line: Int, match: MatchResult) = onMatchFound(line, match.groupValues[group])
}

class StringMatcher(regex: Regex, group: Int) : SingleGroupMatcher(regex, group) {
    var value: String? = null
        private set

    override fun onMatchFound(line: Int, match: String) {
        value = match
    }
}

class StringListMatcher(regex: Regex, group: Int) : SingleGroupMatcher(regex, group) {
    var values: MutableList<String>? = null
    override fun onMatchFound(line: Int, match: String) {
        var result = values
        if (result == null) {
            result = mutableListOf()
            values = result
        }
        result.addAll(match.split(' '))
    }
}

class FlagMatcher(regex: Regex, group: Int) : SingleGroupMatcher(regex, group) {
    var value = false
        private set

    override fun onMatchFound(line: Int, match: String) {
        check(match == "true" || match == "false") { "Boolean value expected: $match" }
        value = match == "true"
    }
}

class CoverageMatcher : SingleGroupMatcher(coverageMarkerRegex, 1) {
    val result = linkedMapOf<Int, String>()
    override fun onMatchFound(line: Int, match: String) {
        result[line] = match
    }
}

class TestTrackingMatcher : SingleGroupMatcher(testTrackingMarkerRegex, 1) {
    val result = linkedMapOf<Int, Set<String>>()
    override fun onMatchFound(line: Int, match: String) {
        result[line] = match.trim().split(' ').toSet()
    }
}

class ExtendedInfoMatcher : Matcher(extendedInfoRegex) {
    val result = linkedMapOf<Int, String>()
    override fun onMatchFound(line: Int, match: MatchResult) {
        val coveredInstructions = match.groupValues[1]
        val totalInstructions = match.groupValues[2]
        val hasBranches = match.groupValues[3].isNotEmpty()
        val branchCoverage = if (hasBranches) " ${match.groupValues[4]}/${match.groupValues[5]}" else ""
        result[line] = "$coveredInstructions/$totalInstructions$branchCoverage"
    }
}
