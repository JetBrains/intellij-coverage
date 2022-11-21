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

package com.intellij.rt.coverage.caseTests

import com.intellij.rt.coverage.*
import com.intellij.rt.coverage.data.ProjectData
import org.junit.Test
import java.io.File

internal abstract class InstructionsBranchesTest(override val coverage: Coverage) : CoverageTest() {
    init {
        commonExtraArgs.add("-Dcoverage.instructions.enable=true")
    }

    override fun verifyResults(projectData: ProjectData, configuration: TestConfiguration, testFile: File) {
        val expected = extractExtendedInfoFromFile(testFile)
        assertEqualsExtendedInfo(projectData, expected, configuration.classes)
    }

    //===GENERATED TESTS===

    @Test
    fun testCasesWhenEnum() = test("cases.whenEnum")

    @Test
    fun testDefaultArgsConstructor() = test("defaultArgs.constructor")

    @Test
    fun testSimpleBranches() = test("simple.branches")

    @Test
    fun testSimpleIfelse() = test("simple.ifelse")

    @Test
    fun testUnloadedCycle() = test("unloaded.cycle")

    @Test
    fun testUnloadedMultiFile() = test("unloaded.multiFile")

    @Test
    fun testUnloadedSingleFile() = test("unloaded.singleFile")

    //===GENERATED TESTS===
}

internal class NewInstructionsTest : InstructionsBranchesTest(Coverage.NEW_BRANCH)
internal class CondyInstructionsTest : InstructionsBranchesTest(Coverage.CONDY_BRANCH)
