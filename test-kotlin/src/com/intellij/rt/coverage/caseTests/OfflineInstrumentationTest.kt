/*
 * Copyright 2000-2022 JetBrains s.r.o.
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
import com.intellij.rt.coverage.instrumentation.UnloadedUtil
import com.intellij.rt.coverage.instrument.OfflineCoverageTransformer
import com.intellij.rt.coverage.instrument.RawReportLoader
import com.intellij.rt.coverage.util.ProcessUtil
import com.intellij.rt.coverage.util.ResourceUtil
import com.intellij.rt.coverage.util.classFinder.ClassFinder
import org.junit.Test
import java.io.File
import java.net.URLClassLoader
import java.util.*
import java.util.regex.Pattern
import kotlin.io.path.createTempDirectory

internal abstract class OfflineInstrumentationTest(override val coverage: Coverage) : CoverageTest() {
    @Test
    fun testCasesJavaSwitch() = test("cases.javaSwitch")

    @Test
    fun testDefaultArgsArgs32() = test("defaultArgs.args32")

    @Test
    fun testDefaultArgsDefaultArgs32() = test("defaultArgs.defaultArgs32")

    @Test
    fun testDefaultArgsInline() = test("defaultArgs.inline")

    @Test
    fun testDefaultArgsSeveralArguments() = test("defaultArgs.severalArguments")

    @Test
    fun testDefaultArgsUncovered() = test("defaultArgs.uncovered")

    @Test
    fun testFixesIDEA_250825() = test("fixes.IDEA_250825")

    @Test
    fun testFixesIDEA_259332() = test("fixes.IDEA_259332")

    @Test
    fun testFixesIntMaxSwitch() = test("fixes.intMaxSwitch")

    @Test
    fun testInlineInlineInline() = test("inline.inlineInline")

    @Test
    fun testInlineSimple() = test("inline.simple")

    @Test
    fun testPropertiesFile() = test("properties.file")

    @Test
    fun testReturnTest() = test("returnTest")

    @Test
    fun testSimpleIfelse() = test("simple.ifelse")


    private fun test(testName: String) {
        val test = getTestFile(testName)
        val configuration = extractTestConfiguration(test.file)
        val config = configuration.setDefaults(test)
        runCoverage(test, config)
    }

    private fun TestConfiguration.setDefaults(test: TestFile): TestConfiguration {
        extraArgs.addAll(commonExtraArgs)
        val fullClassNames = when {
            classes.contains("ALL") || classes.size > 1 -> error("Cannot run offline coverage for several classes")
            classes.isEmpty() -> listOf(test.mainClass)
            else -> classes.map { getFQN(test.testName, it).also { name -> check(name == test.mainClass) { "Classes must include only main class" } } }
        }
        check(patterns == null) { "Offline coverage tests cannot have patterns" }
        check(!calculateUnloaded) { "Offline coverage tests cannot collect unloaded classes" }
        check(extraArgs.isEmpty()) { "Offline coverage tests cannot add extra args" }
        val expected = if (!coverage.isBranchCoverage()) coverageData.mapValues { (_, v) -> if (v == "PARTIAL") "FULL" else v } else coverageData
        return copy(classes = fullClassNames, coverageData = expected)
    }

    private fun runCoverage(test: TestFile, config: TestConfiguration) {
        val rootName = if (test.file.name.endsWith(".kt")) "kotlin" else "java"
        val outputRoot = pathToFile("build", "classes", rootName, "test")

        val includes = Collections.singletonList(Pattern.compile(test.mainClass))
        val excludes = Collections.emptyList<Pattern>()

        runOfflineCoverage(test, outputRoot)

        val projectData = createProjectData(coverage.isBranchCoverage(), includes, excludes)
        val cf = ClassFinder(includes, excludes)
        cf.addClassLoader(URLClassLoader(arrayOf(outputRoot.toURI().toURL())))
        UnloadedUtil.appendUnloaded(projectData, cf, false, coverage.isBranchCoverage())

        RawReportLoader.load(myDataFile, projectData)
        projectData.applyLineMappings()

        assertEqualsLines(projectData, config.coverageData, config.classes)
    }

    private fun runOfflineCoverage(test: TestFile, outputRoot: File) {
        val outputDir = offlineCoverageTransform(coverage.isBranchCoverage(), test, outputRoot)

        val offlineArtifactPath = ResourceUtil.getAgentPath("intellij-coverage-offline")
        val commandLine = arrayOf(
            "-classpath", offlineArtifactPath + File.pathSeparator + outputDir.absolutePath,
            "-Dkover.offline.report.path=${myDataFile.absolutePath}",
            test.mainClass
        )
        ProcessUtil.execJavaProcess(commandLine)
        assertEmptyLogFile(myDataFile)
    }
}

private fun createProjectData(isBranchCoverage: Boolean, includes: List<Pattern>, excludes: List<Pattern>): ProjectData =
    ProjectData(null, isBranchCoverage, null).apply {
        setIncludePatterns(includes)
        excludePatterns = excludes
    }

internal fun offlineCoverageTransform(isBranchCoverage: Boolean, test: TestFile, outputRoot: File): File {
    val className = test.mainClass
    val packageName = className.substring(0, className.lastIndexOf('.'))
    val path = className.replace(".", File.separator) + ".class"
    val parentDir = File(outputRoot, path).parentFile
    val outputDir = createTempDirectory("output").toFile()
    val packageRoot = File(outputDir, packageName.replace(".", File.separator)).apply { mkdirs() }

    val includes = Collections.singletonList(Pattern.compile("$packageName\\..*"))
    val excludes = Collections.emptyList<Pattern>()

    val projectData = createProjectData(isBranchCoverage, includes, excludes)
    val transformer = OfflineCoverageTransformer(projectData, false)

    for (file in parentDir.listFiles()!!) {
        if (!file.isFile) continue
        val name = "$packageName.${file.name.removeSuffix(".class")}"
        val bytes = transformer.transform(ClassLoader.getSystemClassLoader(), name, null, null, file.readBytes())
        File(packageRoot, file.name).writeBytes(bytes)
    }

    return outputDir
}

internal class OfflineLinesInstrumentationTest : OfflineInstrumentationTest(Coverage.LINE)
internal class OfflineBranchesInstrumentationTest : OfflineInstrumentationTest(Coverage.BRANCH)
