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

package com.intellij.rt.coverage

import com.intellij.rt.coverage.caseTests.BranchesTest
import com.intellij.rt.coverage.caseTests.InstructionsBranchesTest
import java.io.File
import kotlin.reflect.KClass

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
    testDataRoot: File,
    ignoredTests: List<String>,
    ignoreCondition: ((TestFile) -> Boolean)? = null
): String {
    require(testDataRoot.isDirectory)
    val testNames = mutableListOf<String>()
    listTestNames("", testDataRoot, ignoredTests, testNames)
    return testNames.mapNotNull { s ->
        try {
            getTestFile(s)
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

private fun getTestFile(ktClass: KClass<*>): File {
    val testClassName = ktClass.qualifiedName!!.split('.').toTypedArray()
    testClassName[testClassName.lastIndex] += ".kt"
    return pathToFile("src", *testClassName)
}

/**
 * test-kotlin should be the test directory.
 */
fun main() {
    generateTests(CoverageRunTest::class)
    generateTests(InstructionsBranchesTest::class) { test ->
        IncludeInstructionsMatcher().also { processFile(test.file, it) }.result
    }
    generateTests(BranchesTest::class) { test ->
        IncludeBranchesMatcher().also { processFile(test.file, it) }.result
    }
}

private fun generateTests(testClass: KClass<*>, ignoreCondition: ((TestFile) -> Boolean)? = null) {
    val ignoredDirectories = listOf("custom")
    val marker = "    //===GENERATED TESTS==="
    val tests = generateTests(File("src", TEST_PACKAGE), ignoredDirectories, ignoreCondition)
    val testFile = getTestFile(testClass)
    replaceGeneratedTests(tests, testFile, marker)
}

private class IgnoreTestMatcher : SingleGroupMatcher<String>(Regex("// ignore: (.*)\$"), 1) {
    override val result get() = ignore
    private var ignore = ""
    override fun onMatchFound(line: Int, match: String) {
        ignore = "    @Ignore(\"$match\")\n"
    }
}

private open class DirectiveMatcher(directive: String) : Matcher<Boolean>(Regex(directive)) {
    private var matchFound = false
    override val result get() = matchFound
    override fun onMatchFound(line: Int, match: MatchResult) {
        matchFound = true
    }
}

private class IncludeInstructionsMatcher : DirectiveMatcher("// instructions & branches")
private class IncludeBranchesMatcher : DirectiveMatcher("// with branches")

