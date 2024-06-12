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

package com.intellij.rt.coverage

import java.io.File

private val testFileNames = listOf("test.kt", "Test.java")

private fun listTestNames(name: String, root: File, ignoredTests: List<String>, result: MutableList<String>) {
    if (name in ignoredTests) return
    val children = root.list()?.sorted()?.map { File(root, it) }
        ?: error("Cannot list files in ${root.path}")

    if (children.all { !it.isDirectory }) {
        result.add(name)
        return
    }

    val prefix = if (name.isEmpty()) "" else "$name."
    for (child in children) {
        if (child.isDirectory) {
            listTestNames("$prefix${child.name}", child, ignoredTests, result)
        } else {
            check(child.name !in testFileNames) { "Test source should be located in a separate subfolder: ${child.path}." }
        }
    }
}

/**
 * Generate test methods code from test sources located in [testDataRoot].
 *
 * Test method includes Test annotation and test method call. Test name is capitalized in the test method name.
 * A test can be ignored by adding '// ignore: REASON' comment in the main source file.
 *
 * @param ignoredTests list of test names that should be ignored, for example "custom" tests
 */
private fun generateTests(
    module: String,
    testDataRoot: File,
    ignoredTests: List<String>,
    ignoreCondition: ((TestFile) -> Boolean)? = null
): String {
    require(testDataRoot.isDirectory)
    val testNames = mutableListOf<String>()
    listTestNames("", testDataRoot, ignoredTests, testNames)
    return testNames.mapNotNull { s ->
        try {
            getTestFile(s, module)
        } catch (e: Throwable) {
            println("Error in $s: ${e.message}")
            null
        }
    }
        .let { tests -> ignoreCondition?.let { tests.filter(it) } ?: tests }
        .joinToString("\n") { test ->
            val ignored = IgnoreTestMatcher()
            processFile(test.file, ignored)

            val capitalized = test.testName.split('.').joinToString("") { it.capitalize() }
            "    @Test\n${ignored.result}    fun test$capitalized() = test(\"${test.testName}\")\n"
        }
}

/**
 * Replace test between two substrings equal to [marker] with generated tests code.
 */
fun replaceGeneratedTests(tests: String, testFile: File, marker: String) {
    val fileContent = testFile.readText()
    val start = fileContent.indexOf(marker).also { check(it >= 0) }.let { it + marker.length }
    val end = fileContent.lastIndexOf(marker).also { check(it >= 0) }
    val newContent = """
        |${fileContent.substring(0, start)}
        |
        |$tests
        |${fileContent.substring(end)}
    """.trimMargin()
    testFile.writeText(newContent)
}

/**
 * test-kotlin should be the test directory.
 */
fun main() {
    generateTests("../test-sources", "test/com/intellij/rt/coverage/CoverageRunTest.kt")
    generateTests("../test-sources11", "test/com/intellij/rt/coverage/Jdk11CoverageRunTest.kt")
    generateTests("../test-sources","test/com/intellij/rt/coverage/caseTests/InstructionsBranchesTest.kt") { test ->
        hasDirective(IncludeInstructionsMatcher(), test)
    }
    generateTests("../test-sources","test/com/intellij/rt/coverage/caseTests/OfflineInstrumentationTest.kt") { test ->
        hasDirective(OfflineInstrumentationMatcher(), test)
    }
}

private fun hasDirective(
    matcher: Matcher<Boolean>,
    test: TestFile
) = matcher.also { processFile(test.file, it) }.result

private fun generateTests(module: String, testFilePath: String, ignoreCondition: ((TestFile) -> Boolean)? = null) {
    val ignoredDirectories = listOf("custom")
    val marker = "    //===GENERATED TESTS==="
    val tests = generateTests(module, pathToFile(module, "src", TEST_PACKAGE), ignoredDirectories, ignoreCondition)
    val testFile = File(module, testFilePath)
    replaceGeneratedTests(tests, testFile, marker)
}

private class IncludeInstructionsMatcher : DirectiveMatcher("// instructions & branches")
private class OfflineInstrumentationMatcher : DirectiveMatcher("// offline instrumentation")
private class IgnoreTestMatcher : SingleGroupMatcher<String>(Regex("// ignore: (.*)\$"), 1) {
    override val result get() = ignore
    private var ignore = ""
    override fun onMatchFound(line: Int, match: String) {
        ignore = "    @Ignore(\"$match\")\n"
    }
}

