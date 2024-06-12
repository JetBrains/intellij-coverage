/*
 * Copyright 2000-2024 JetBrains s.r.o.
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

package com.intellij.rt.coverage.caseTests

import com.intellij.rt.coverage.createTmpFile
import com.intellij.rt.coverage.data.ProjectData
import com.intellij.rt.coverage.instrumentation.InstrumentationOptions
import com.intellij.rt.coverage.logFile
import com.intellij.rt.coverage.util.CoverageIOUtil
import com.intellij.rt.coverage.util.CoverageReport
import com.intellij.rt.coverage.util.ErrorReporter
import com.intellij.rt.coverage.util.ProjectDataLoader
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern

class ReportFormatTest {
    private lateinit var file: File
    private lateinit var out: DataOutputStream

    @Before
    fun setUp() {
        file = createTmpFile(".ic")
        ErrorReporter.suggestBasePath(file.parent)
        out = DataOutputStream(FileOutputStream(file))
    }

    @After
    fun tearDown() {
        out.close()
        logFile(file)?.delete()
        ErrorReporter.setPath(null)
        file.delete()
    }

    private fun writeSimpleProject() {
        CoverageIOUtil.writeINT(out, 1) // class count
        CoverageIOUtil.writeUTF(out, "ClassName")
        CoverageIOUtil.writeINT(out, 0) // class index
        CoverageIOUtil.writeINT(out, 1) // method count
        CoverageIOUtil.writeUTF(out, "methodName()V")
        CoverageIOUtil.writeINT(out, 2) // line count

        CoverageIOUtil.writeINT(out, 42) // line number
        CoverageIOUtil.writeUTF(out, "") // test name
        CoverageIOUtil.writeINT(out, 1) // hits
        CoverageIOUtil.writeINT(out, 1) // jumps count
        CoverageIOUtil.writeINT(out, 1) // true hits
        CoverageIOUtil.writeINT(out, 0) // false hits
        CoverageIOUtil.writeINT(out, 0) // switches count

        CoverageIOUtil.writeINT(out, 43) // line number
        CoverageIOUtil.writeUTF(out, "") // test name
        CoverageIOUtil.writeINT(out, 0) // hits
        out.flush()
    }

    @Test
    fun testReadOldFormat() {
        writeSimpleProject()
        ProjectDataLoader.load(file)
        readLog()?.also { log -> throw RuntimeException(log) }
    }

    @Test
    fun testNewSectionsVersion() {
        writeSimpleProject()

        CoverageIOUtil.writeINT(out, Int.MAX_VALUE) // unsupported version number
        // CoverageIOUtil.writeUTF(out, "") // no extra line
        CoverageIOUtil.writeINT(out, 0)
        out.flush()

        ProjectDataLoader.load(file)
        Assert.assertNotNull(readLog())
    }

    private fun readLog(): String? = logFile(file)?.readText()

    @Test
    fun testUnknownSection() {
        writeSimpleProject()

        CoverageIOUtil.writeINT(out, 1) // version
        CoverageIOUtil.writeUTF(out, "") // extra line
        CoverageIOUtil.writeINT(out, 2) // sections number

        CoverageIOUtil.writeINT(out, Int.MAX_VALUE) // unknown section id
        CoverageIOUtil.writeINT(out, 3) // size
        CoverageIOUtil.writeINT(out, 0) // version
        out.write(byteArrayOf(1, 2, 3)) // data

        CoverageIOUtil.writeINT(out, 1) // section id
        CoverageIOUtil.writeINT(out, -1) // size
        CoverageIOUtil.writeINT(out, 0) // version
        CoverageIOUtil.writeINT(out, 0) // class id
        CoverageIOUtil.writeINT(out, 1) // jumps count
        CoverageIOUtil.writeINT(out, 0) // switches count
        CoverageIOUtil.writeINT(out, -1) // end

        out.flush()

        val projectData = ProjectDataLoader.load(file)
        Assert.assertNotNull(readLog())
        Assert.assertEquals(1, projectData.getClassData("ClassName").getLineData(43).jumpsCount())
    }

    @Test
    fun testUnknownSectionVersion() {
        writeSimpleProject()

        CoverageIOUtil.writeINT(out, 1) // version
        CoverageIOUtil.writeUTF(out, "") // extra line
        CoverageIOUtil.writeINT(out, 2) // sections number

        CoverageIOUtil.writeINT(out, 1) // section id
        CoverageIOUtil.writeINT(out, 3) // size
        CoverageIOUtil.writeINT(out, Int.MAX_VALUE) // unsupported version
        out.write(byteArrayOf(1, 2, 3)) // data

        CoverageIOUtil.writeINT(out, 1) // section id
        CoverageIOUtil.writeINT(out, -1) // size
        CoverageIOUtil.writeINT(out, 0) // version
        CoverageIOUtil.writeINT(out, 0) // class id
        CoverageIOUtil.writeINT(out, 1) // jumps count
        CoverageIOUtil.writeINT(out, 0) // switches count
        CoverageIOUtil.writeINT(out, -1) // end

        out.flush()

        val projectData = ProjectDataLoader.load(file)
        Assert.assertNotNull(readLog())
        Assert.assertEquals(1, projectData.getClassData("ClassName").getLineData(43).jumpsCount())
    }

    @Test
    fun testIncludeFiltersPersist() {
        val includeFilters = listOf(Pattern.compile("a.*"))
        val excludeFilters = listOf(Pattern.compile("a.b.*"))
        val annotations = listOf(Pattern.compile("a.b.C.*\$"))

        val projectData = ProjectData()
        projectData.includePatterns = includeFilters
        projectData.excludePatterns = excludeFilters
        projectData.annotationsToIgnore = annotations
        CoverageReport.save(projectData, InstrumentationOptions.Builder().setDataFile(file).build())

        val readProjectData = ProjectDataLoader.load(file)
        Assert.assertEquals(includeFilters.toString(), readProjectData.includePatterns.toString())
        Assert.assertEquals(excludeFilters.toString(), readProjectData.excludePatterns.toString())
        Assert.assertEquals(annotations.toString(), readProjectData.annotationsToIgnore.toString())
    }
}