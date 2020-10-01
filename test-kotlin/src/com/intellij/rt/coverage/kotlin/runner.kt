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

package com.intellij.rt.coverage.kotlin

import com.intellij.rt.coverage.data.ClassData
import com.intellij.rt.coverage.data.LineData
import com.intellij.rt.coverage.data.ProjectData
import com.intellij.rt.coverage.util.FileUtil
import com.intellij.rt.coverage.util.ProcessUtil
import com.intellij.rt.coverage.util.ProjectDataLoader
import com.intellij.rt.coverage.util.ResourceUtil
import junit.framework.TestCase.assertEquals
import java.io.File


enum class LineStatus {
    NONE, PARTIAL, FULL
}

fun runWithCoverage(coverageDataFile: File, testName: String, sampling: Boolean): ProjectData {
    val coverageAgentPath = ResourceUtil.getAgentPath("intellij-coverage-agent")
    val classPath = System.getProperty("java.class.path")
    val commandLine = arrayOf("""-javaagent:$coverageAgentPath="${coverageDataFile.path}" false false false $sampling""",
            "-classpath", classPath,
            "kotlinTestData.Main",
            testName
    )
    ProcessUtil.execJavaProcess(commandLine)
    FileUtil.waitUntilFileCreated(coverageDataFile)
    return ProjectDataLoader.load(coverageDataFile)!!
}

fun ProjectData.assertEqualsClassLines(className: String, expectedLines: Map<Int, LineStatus>) {
    val classData = getClassData(className)!!
    val lines = classData.getLinesData().associateBy({ it.lineNumber }, { createLineStatus(it.status) })
    assertEquals(expectedLines, lines)
}

private fun createLineStatus(value: Int) = when (value) {
    0 -> LineStatus.NONE
    1 -> LineStatus.PARTIAL
    else -> LineStatus.FULL
}

private fun ClassData.getLinesData() = lines.filterIsInstance(LineData::class.java).sortedBy { it.lineNumber }
