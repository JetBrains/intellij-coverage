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

package com.intellij.rt.coverage.caseTests

import com.intellij.rt.coverage.Coverage
import com.intellij.rt.coverage.CoverageTest
import com.intellij.rt.coverage.TestConfiguration
import com.intellij.rt.coverage.data.ProjectData
import org.junit.Test

internal class ArtifactsTest : CoverageTest() {
    override val coverage: Coverage
        get() = Coverage.LINE

    override fun verifyResults(projectData: ProjectData, configuration: TestConfiguration) {
    }

    @Test
    fun `test IntelliJ API`() = test("custom.api.intellij")
}
