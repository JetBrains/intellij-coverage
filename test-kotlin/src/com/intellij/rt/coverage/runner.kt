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
import org.junit.Assert.assertEquals
import java.io.File
import java.nio.file.Paths


fun runWithCoverage(coverageDataFile: File, testName: String, sampling: Boolean): ProjectData {
    val classPath = System.getProperty("java.class.path")
    return CoverageStatusTest.runCoverage(classPath, coverageDataFile, "kotlinTestData.*", "kotlinTestData.$testName.Test", sampling)
}

internal fun assertEqualsLines(project: ProjectData, expectedLines: Map<Int, String>, classNames: List<String>) {
    val allData = ClassData("")
    classNames
            .map { project.getClassData(it) }
            .forEach { allData.merge(it) }
    val lines = allData.getLinesData().associateBy({ it.lineNumber }, { it.status.toByte() })
    assertEquals(expectedLines, statusToString(lines))
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
