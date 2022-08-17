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
import org.junit.Assert
import org.junit.Test
import java.io.File

internal class LineSignatureTest : CoverageTest() {
    @Test
    fun test_IDEA_275520Loaded() = test("custom.IDEA_275520.loaded", verify = { projectData, _, _ ->
        test_IDEA_275520(projectData)
    })

    @Test
    fun test_IDEA_275520Unloaded() = test("custom.IDEA_275520.unloaded", verify = { projectData, _, _ ->
        test_IDEA_275520(projectData)
    })

    @Test
    fun test_IDEA_275520Call() = test("custom.IDEA_275520.call", verify = { projectData, _, _ ->
        test_IDEA_275520(projectData, hashMapOf(
                "$TEST_PACKAGE.custom.IDEA_275520.call.TestKt" to hashMapOf(
                        "main()V" to 37..39),
                "$TEST_PACKAGE.custom.IDEA_275520.call.UnloadedObject" to hashMapOf(
                        "foo(Lkotlin/jvm/functions/Function1;)V" to 25..25,
                        "boo(Lkotlin/jvm/functions/Function1;)V" to 30..30)))
    })

    @Test
    fun test_IDEA_275520Stdlib() = test("custom.IDEA_275520.stdlib", verify = { projectData, _, _ ->
        test_IDEA_275520(projectData, hashMapOf(
                "$TEST_PACKAGE.custom.IDEA_275520.stdlib.TestKt" to hashMapOf(
                        "main()V" to 23..27,
                        "reindent(Ljava/util/List;)Ljava/lang/String;" to 31..34,
                        "foo()V" to 38..40)))
    })

    private fun test_IDEA_275520(projectData: ProjectData, classes: Map<String, Map<String, IntRange>> = hashMapOf(
            "$TEST_PACKAGE.custom.IDEA_275520.Test2Kt" to hashMapOf(
                    "simpleInline(I)V" to 20..20,
                    "nestedInlines(I)V" to 24..28,
                    "oneLineInline()I" to 31..31,
                    "withLambda(Lkotlin/jvm/functions/Function0;)I" to 34..34,
                    "testWithLambda()V" to 38..38))) {
        for ((className, signatures) in classes) {
            val classData = projectData.getClassData(className)
            val expected = signatures.entries.joinToString("\n")
            { (s, lines) -> lines.joinToString("\n") { "$it $s" } }

            val actual = signatures.entries.joinToString("\n")
            { (_, lines) -> lines.joinToString("\n") { "$it ${classData.getLineData(it)?.methodSignature}" } }

            Assert.assertEquals(expected, actual)
        }
    }

    override val coverage get() = Coverage.NEW_SAMPLING
    override fun verifyResults(projectData: ProjectData, configuration: TestConfiguration, testFile: File) {
        assertEqualsLines(projectData, configuration.coverageData, configuration.classes)
    }
}
