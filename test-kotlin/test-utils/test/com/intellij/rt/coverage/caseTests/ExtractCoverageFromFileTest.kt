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

import com.intellij.rt.coverage.*
import com.intellij.rt.coverage.extractTestConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

internal class ExtractCoverageFromFileTest {

    @Test
    fun extractCoverageDataFromFile() {
        val expected = mapOf(
            4 to "FULL",
            5 to "PARTIAL",
            6 to "NONE",
            10 to "FULL"
        )
        val path = this.javaClass.classLoader.getResource("extractionTest.txt")!!.toURI().toURL().file
        val testFile = File(path)
        val testConfiguration = extractTestConfiguration(testFile)
        assertEquals(listOf("A", "B", "C", "D"), testConfiguration.classes)
        assertEquals(listOf("-hello", "bye"), testConfiguration.extraArgs)
        assertTrue(testConfiguration.calculateUnloaded)
        assertEquals(expected, TestResult.CoverageResults.collectExpectedData(testConfiguration, Coverage.BRANCH))
    }
}
