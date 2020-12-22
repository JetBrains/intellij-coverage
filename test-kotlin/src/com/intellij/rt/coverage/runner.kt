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
import org.junit.Assert
import java.io.File
import java.nio.file.Paths


fun runWithCoverage(coverageDataFile: File, testName: String, sampling: Boolean, calcUnloaded: Boolean = false): ProjectData {
    val classPath = System.getProperty("java.class.path")
    return CoverageStatusTest.runCoverage(classPath, coverageDataFile, "kotlinTestData.*", "kotlinTestData.$testName.Test", sampling, calcUnloaded)
}

internal fun assertEqualsLines(project: ProjectData, expectedLines: Map<Int, String>, classNames: List<String>) {
    val actualCoverage = coverageLines(project, classNames)
    logCoverageDiff(expectedLines, actualCoverage)
    Assert.assertEquals(expectedLines, actualCoverage)
}

internal const val all = "ALL CLASSES"

private fun coverageLines(project: ProjectData, classNames: List<String>): Map<Int, String> {
    val allData = ClassData("")
    if (classNames.contains(all)) {
        project.classes.values.forEach { allData.merge(it) }
    } else {
        classNames
                .map { project.getClassData(it) }
                .forEach { allData.merge(it) }
    }
    val lines = allData.getLinesData().associateBy({ it.lineNumber }, { it.status.toByte() })
    return statusToString(lines)
}

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

private fun statusToString(lines: Map<Int, Byte>) = lines.mapValues {
    when (it.value.toInt()) {
        0 -> "NONE"
        1 -> "PARTIAL"
        else -> "FULL"
    }
}

private fun ClassData.getLinesData() = lines.filterIsInstance(LineData::class.java).sortedBy { it.lineNumber }

private val coverageMarkerRegex = Regex("// coverage: (FULL|PARTIAL|NONE)( .*)?\$")

internal fun extractCoverageDataFromFile(file: File): Map<Int, String> = file.bufferedReader()
        .lineSequence()
        .mapIndexed { index, s -> index + 1 to coverageMarkerRegex.find(s) }
        .filter { it.second != null }
        .toMap()
        .mapValues { it.value!!.groupValues[1] }

internal fun pathToFile(name: String, vararg names: String): File = Paths.get(name, *names).toFile()
