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
import com.intellij.rt.coverage.util.TestTrackingIOUtil
import org.junit.Assert
import java.io.File
import java.nio.file.Paths

internal enum class Coverage {
    SAMPLING, NEW_SAMPLING, TRACING, NEW_TRACING, CONDY_SAMPLING, CONDY_TRACING
}

internal fun runWithCoverage(coverageDataFile: File, testName: String, coverage: Coverage, calcUnloaded: Boolean = false, testTracking: Boolean = false,
                    patterns: String = "$TEST_PACKAGE.*", extraArgs: MutableList<String> = mutableListOf(),
                    mainClass: String = getTestFile(testName).mainClass): ProjectData {
    val classPath = System.getProperty("java.class.path")
    when (coverage) {
        Coverage.CONDY_SAMPLING -> extraArgs.add("-Didea.new.sampling.coverage=true")
        Coverage.CONDY_TRACING -> extraArgs.add("-Didea.new.tracing.coverage=true")
        Coverage.NEW_SAMPLING -> {
            extraArgs.add("-Didea.new.sampling.coverage=true")
            extraArgs.add("-Dcoverage.condy.enable=false")
        }
        Coverage.NEW_TRACING -> {
            extraArgs.add("-Didea.new.tracing.coverage=true")
            extraArgs.add("-Dcoverage.condy.enable=false")
        }
    }
    val sampling = coverage == Coverage.SAMPLING || coverage == Coverage.NEW_SAMPLING || coverage == Coverage.CONDY_SAMPLING
    return CoverageStatusTest.runCoverage(classPath, coverageDataFile, patterns, mainClass,
            sampling, extraArgs.toTypedArray(), calcUnloaded, testTracking)
            .also {
                logFile(coverageDataFile)?.readText()?.also { log-> throw RuntimeException(log) }
            }
}

internal fun assertEqualsLines(project: ProjectData, expectedLines: Map<Int, String>, classNames: List<String>) {
    val actualCoverage = coverageLines(project, classNames).associate { it.lineNumber to it.status }.statusToString()
    logCoverageDiff(expectedLines, actualCoverage)
    Assert.assertEquals(expectedLines, actualCoverage)
}

internal fun assertEqualsTestTracking(coverageDataFile: File, expected: Map<Int, Set<String>>, classNames: List<String>) {
    val actual = testTrackingLines(coverageDataFile, classNames)
    Assert.assertEquals(expected, actual)
}

internal fun assertEqualsExtendedInfo(project: ProjectData, expected: Map<Int, String>, classNames: List<String>) {
    val actual = extendedLineInfo(project, classNames)
    Assert.assertEquals(expected.map { it.toString() }.joinToString("\n"), actual.map { it.toString() }.joinToString("\n"))
}

internal const val all = "ALL CLASSES"

private fun coverageLines(project: ProjectData, classNames: List<String>): List<LineData> {
    val allData = ClassData("")
    getClasses(classNames, project).forEach { allData.merge(it) }
    return allData.getLinesData()
}

private fun extendedLineInfo(project: ProjectData, classNames: List<String>) = coverageLines(project, classNames)
        .run {
            val instructionsMap = hashMapOf<Int, LineInstructions>()
            getClasses(classNames, project).forEach { classData ->
                val classInstructions = project.instructions[classData.name]!!
                classData.lines.filterIsInstance<LineData>().forEach { instructionsMap.getOrPut(it.lineNumber) { LineInstructions()}.merge(classInstructions.getlines()[it.lineNumber]) }
            }
            associate { line ->
                val branches = line.branchData
                val instructions = instructionsMap[line.lineNumber]!!.getInstructionsData(line)
                line.lineNumber to "${instructions.coveredBranches}/${instructions.totalBranches} ${branches?.coveredBranches ?: 0}/${branches?.totalBranches ?: 0}"
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
    val tracesDir = ProjectData.createTracesDir(coverageDataFile)
    return try {
        TestTrackingIOUtil.loadTestTrackingData(tracesDir)
    } finally {
        tracesDir.deleteRecursively()
    }
}

internal fun getLineHits(data: ClassData, line: Int) = data.getLinesData().single { it.lineNumber == line }.hits

private fun logCoverageDiff(expectedLines: Map<Int, String>, actualCoverage: Map<Int, String>) {
    val expected = expectedLines.toList()
    val actual = actualCoverage.toList()
    compareCoverage(expected, actual, wrongLineCoverage = { i, j ->
        System.err.println("Line ${expected[i].first}: expected ${expected[i].second} but ${actual[j].second} found")
    }, missedLine = { i ->
        System.err.println("Line ${expected[i].first} expected with coverage ${expected[i].second}")
    }, unexpectedLine = { i ->
        System.err.println("Unexpected line ${actual[i].first} with coverage ${actual[i].second}")
    })
}

private fun compareCoverage(expected: List<Pair<Int, String>>,
                            actual: List<Pair<Int, String>>,
                            wrongLineCoverage: (Int, Int) -> Unit,
                            missedLine: (Int) -> Unit,
                            unexpectedLine: (Int) -> Unit) {
    var j = 0
    var i = 0
    while (i < expected.size && j < actual.size) {
        if (expected[i].first == actual[j].first) {
            if (expected[i].second != actual[j].second) {
                wrongLineCoverage(i, j)
            }
            i++
            j++
        } else if (expected[i].first < actual[j].first) {
            missedLine(i++)
        } else {
            unexpectedLine(j++)
        }
    }
    while (i < expected.size) missedLine(i++)
    while (j < actual.size) unexpectedLine(j++)
}

private fun Map<Int, Int>.statusToString() = mapValues {
    when (it.value) {
        0 -> "NONE"
        1 -> "PARTIAL"
        else -> "FULL"
    }
}

private fun ClassData.getLinesData() = (lines ?: Array<LineData?>(0) { null })
        .filterIsInstance(LineData::class.java).sortedBy { it.lineNumber }

internal fun extractTestTrackingDataFromFile(file: File): Map<Int, Set<String>> =
    TestTrackingMatcher().let { callback ->
        processFile(file, callback)
        callback.result
    }

internal fun extractExtendedInfoFromFile(file: File): Map<Int, String> = ExtendedInfoMatcher().let { matcher ->
    val otherFile = StringMatcher(fileWithCoverageMarkersRegex, 1)
    processFile(file, otherFile)
    if (otherFile.value !== null) {
        val fileWithMarkers = File(file.parentFile, otherFile.value!!)
        processFile(fileWithMarkers, matcher)
    } else {
        processFile(file, matcher)
    }
    matcher.result
}

internal fun pathToFile(name: String, vararg names: String): File = Paths.get(name, *names).toFile()

internal fun extractTestConfiguration(file: File): TestConfiguration {
    var coverage = CoverageMatcher()
    val classes = StringListMatcher(classesMarkerRegex, 1)
    val extraArgs = StringListMatcher(extraArgumentsMarkerRegex, 1)
    val patterns = StringMatcher(patternsMarkerRegex, 1)
    val calculateUnloaded = FlagMatcher(calculateUnloadedMarkerRegex, 1)
    val otherFile = StringMatcher(fileWithCoverageMarkersRegex, 1)

    processFile(file, coverage, classes, extraArgs, patterns, calculateUnloaded, otherFile)
    if (otherFile.value !== null) {
        coverage = CoverageMatcher()
        val fileWithMarkers = File(file.parentFile, otherFile.value!!)
        processFile(fileWithMarkers, coverage)
    }
    return TestConfiguration(
        coverage.result,
        classes.values,
        patterns.value,
        extraArgs.values,
        calculateUnloaded.value
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

private val coverageMarkerRegex = Regex("// coverage: (FULL|PARTIAL|NONE)( .*)?\$")
private val classesMarkerRegex = Regex("// classes: (.*)\$")
private val patternsMarkerRegex = Regex("// patterns: (.*)\$")
private val calculateUnloadedMarkerRegex = Regex("// calculate unloaded: (.*)\$")
private val extraArgumentsMarkerRegex = Regex("// extra args: (.*)\$")
private val testTrackingMarkerRegex = Regex("// tests: ([^/]*)")
private val fileWithCoverageMarkersRegex = Regex("// markers: (.*)\$")
private val extendedInfoRegex = Regex("// stats: ([0-9]+)/([0-9]+)(\\s+([0-9]+)/([0-9]+))?")

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
    val values = mutableListOf<String>()
    override fun onMatchFound(line: Int, match: String) {
        values.addAll(match.split(' '))
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
        val coveredBranches = if (hasBranches) match.groupValues[4] else 0
        val totalBranches = if (hasBranches) match.groupValues[5] else 0
        result[line] = "$coveredInstructions/$totalInstructions $coveredBranches/$totalBranches"
    }
}
