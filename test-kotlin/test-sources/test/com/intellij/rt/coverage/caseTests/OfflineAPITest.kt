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
import com.intellij.rt.coverage.util.OptionsUtil
import com.intellij.rt.coverage.util.ProcessUtil
import com.intellij.rt.coverage.util.ResourceUtil
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File

class OfflineAPITest {
    private lateinit var myDataFile: File

    @Before
    fun setUp() {
        myDataFile = createTmpFile(".txt")
    }

    @After
    fun tearDown() {
        myDataFile.delete()
    }

    @Test
    fun testBasic() = test("custom.offline.api.basic")

    @Test
    fun testBasicSwitch() = test("custom.offline.api.basicSwitch")

    @Test
    fun testConstructor() = test("custom.offline.api.constructor")


    private fun test(testName: String) = runWithOptions(mapOf(
        OptionsUtil::CALCULATE_HITS_COUNT to true
    )) {
        val test = getTestFile(testName)
        val outputRoot = pathToFile("build", "classes", "kotlin", "main")
        val outputDir = offlineCoverageTransform(true, test, outputRoot)

        val offlineArtifactPath = ResourceUtil.getAgentPath(pathToFile("..", "..", "dist"),"intellij-coverage-offline")
        val classpath = System.getProperty("java.class.path").split(File.pathSeparator).asSequence()
            .filter { path -> path.contains("kotlin-stdlib") }
            .plus(offlineArtifactPath).plus(outputDir.absolutePath).plus(outputRoot.absolutePath)
            .joinToString(File.pathSeparator)
        val commandLine = arrayOf(
            "-classpath", classpath,
            "-Dcoverage.test.result.path=${myDataFile.absolutePath}",
            test.mainClass
        )
        ProcessUtil.execJavaProcess(commandLine)
        assertEmptyLogFile(myDataFile)

        val expectedFileName = "${testName.replace('.', '/')}.txt"
        val expected = File(this::class.java.classLoader.getResource(expectedFileName)!!.path).readText()
        val actual = myDataFile.readText()
        Assert.assertEquals(expected.toSystemIndependent(), actual.toSystemIndependent())
    }
}
