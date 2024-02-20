/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

import com.intellij.rt.coverage.data.LineData
import com.intellij.rt.coverage.data.ProjectData
import com.intellij.rt.coverage.instrument.RawReportLoader
import org.junit.Assert
import org.junit.Test
import java.nio.file.Files

class RawHitsReportTest {
    @Test
    fun testReportSave() {
        val rawProjectData = RawProjectData()
        val hits = rawProjectData.getOrCreateClass("A", 2, true).hits as IntArray
        hits[0] = 2
        hits[1] = 3

        val file = Files.createTempFile("coverage", "ric").toFile()
        RawHitsReport.dump(file, rawProjectData)

        val projectData = createProject()
        val classData = projectData.getClassData("A")
        classData.setHitsMask(hits)
        classData.applyHits()

        val loadedProjectData = createProject()
        RawReportLoader.load(file, loadedProjectData)

        val loadedClassData = loadedProjectData.getClassData("A")
        for (i in classData.lines.indices) {
            Assert.assertEquals(classData.getLineData(i).hits.toLong(), loadedClassData.getLineData(i).hits.toLong())
        }
    }
}

private fun createProject(): ProjectData {
    val projectData = ProjectData()
    val classData = projectData.getOrCreateClassData("A")
    classData.setLines(
        arrayOf(
            LineData(0, "a()"),
            LineData(1, "a()"),
        )
    )
    return projectData
}
