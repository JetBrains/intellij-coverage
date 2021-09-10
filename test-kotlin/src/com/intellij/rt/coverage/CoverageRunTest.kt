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

import com.intellij.rt.coverage.data.ProjectData
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import testData.custom.threadSafe.data.THREAD_SAFE_DATA_EXPECTED_HITS
import testData.custom.threadSafe.structure.THREAD_SAFE_STRUCTURE_CLASSES
import java.io.File

/**
 * Tests between ===GENERATED TESTS=== marker are generated automatically by testGeneration.kt code.
 * @see [generateTests]
 */
internal abstract class CoverageRunTest : CoverageTest() {
    //===GENERATED TESTS===

    @Test
    fun testBadCycleClasses() = test("badCycle.classes")

    @Test
    fun testBadCycleInterfaces() = test("badCycle.interfaces")

    @Test
    fun testCoroutinesAsync() = test("coroutines.async")

    @Test
    @Ignore("To be fixed")
    fun testCoroutinesFix1() = test("coroutines.fix1")

    @Test
    fun testCoroutinesFunction() = test("coroutines.function")

    @Test
    fun testCoroutinesInline() = test("coroutines.inline")

    @Test
    fun testCoroutinesLambda() = test("coroutines.lambda")

    @Test
    fun testCoroutinesNoSuspend() = test("coroutines.noSuspend")

    @Test
    fun testCoroutinesNonVoid() = test("coroutines.nonVoid")

    @Test
    fun testCoroutinesTailSuspendCall() = test("coroutines.tailSuspendCall")

    @Test
    fun testDataClass() = test("dataClass")

    @Test
    fun testDefaultArgsArgs32() = test("defaultArgs.args32")

    @Test
    fun testDefaultArgsCovered() = test("defaultArgs.covered")

    @Test
    fun testDefaultArgsDefaultArgs32() = test("defaultArgs.defaultArgs32")

    @Test
    fun testDefaultArgsInline() = test("defaultArgs.inline")

    @Test
    fun testDefaultArgsSeveralArguments() = test("defaultArgs.severalArguments")

    @Test
    fun testDefaultArgsTracing() = test("defaultArgs.tracing")

    @Test
    fun testDefaultArgsUncovered() = test("defaultArgs.uncovered")

    @Test
    fun testDefaultInterfaceMemberJava() = test("defaultInterfaceMember.java")

    @Test
    fun testDefaultInterfaceMemberKotlin() = test("defaultInterfaceMember.kotlin")

    @Test
    fun testDefaultInterfaceMemberRemoveOnlyDefaultInterfaceMember() = test("defaultInterfaceMember.removeOnlyDefaultInterfaceMember")

    @Test
    fun testFixesIDEA_250825() = test("fixes.IDEA_250825")

    @Test
    fun testFixesIDEA_258370() = test("fixes.IDEA_258370")

    @Test
    fun testFixesIDEA_259332() = test("fixes.IDEA_259332")

    @Test
    fun testFixesIDEA_259731() = test("fixes.IDEA_259731")

    @Test
    fun testFixesIDEA_264534() = test("fixes.IDEA_264534")

    @Test
    fun testFixesIDEA_268006Exclude() = test("fixes.IDEA_268006.exclude")

    @Test
    fun testFixesIDEA_268006NoInclude() = test("fixes.IDEA_268006.noInclude")

    @Test
    fun testFixesKT_39038() = test("fixes.KT_39038")

    @Test
    fun testFunInterface() = test("funInterface")

    @Test
    fun testImplementationByDelegation() = test("implementationByDelegation")

    @Test
    fun testImplementationByDelegationGeneric() = test("implementationByDelegationGeneric")

    @Test
    fun testInlineCoroutines() = test("inline.coroutines")

    @Test
    fun testInlineCrossinline() = test("inline.crossinline")

    @Test
    fun testInlineInlineInline() = test("inline.inlineInline")

    @Test
    fun testInlineLambda() = test("inline.lambda")

    @Test
    fun testInlineNoinline() = test("inline.noinline")

    @Test
    fun testInlineReflection() = test("inline.reflection")

    @Test
    fun testInlineReified() = test("inline.reified")

    @Test
    fun testInlineSimple() = test("inline.simple")

    @Test
    fun testInlineWithReturn() = test("inline.withReturn")

    @Test
    fun testInterfaceWithClinit() = test("interfaceWithClinit")

    @Test
    fun testJavaSwitch() = test("javaSwitch")

    @Test
    fun testPropertiesConstructor() = test("properties.constructor")

    @Test
    fun testPropertiesFile() = test("properties.file")

    @Test
    @Ignore("Not implemented")
    fun testPropertiesGetterAndSetter() = test("properties.getterAndSetter")

    @Test
    fun testRedefine() = test("redefine")

    @Test
    fun testReturnTest() = test("returnTest")

    @Test
    fun testSealedClassConstructor() = test("sealedClassConstructor")

    @Test
    fun testSimpleIfelse() = test("simple.ifelse")

    @Test
    fun testUnloadedInline() = test("unloaded.inline")

    @Test
    fun testUnloadedSingleFile() = test("unloaded.singleFile")

    @Test
    fun testUtilClassJava() = test("utilClass.java")

    @Test
    fun testUtilClassKotlin() = test("utilClass.kotlin")

    @Test
    fun testWhenMapping() = test("whenMapping")

    //===GENERATED TESTS===

    @Test
    fun testThreadSafeStructure() = test(
        "custom.threadSafe.structure",
        configuration = TestConfiguration((1..THREAD_SAFE_STRUCTURE_CLASSES).associateWith { "FULL" },
            (0 until THREAD_SAFE_STRUCTURE_CLASSES).map { "Class$it" })
    )

    @Test
    @Ignore("Coverage hit increment is not atomic.")
    fun testThreadSafeData() = test("custom.threadSafe.data", verify = { projectData, _, _ ->
        val classData = projectData.getClassData("testData.custom.threadSafe.data.SimpleClass")
        Assert.assertEquals(THREAD_SAFE_DATA_EXPECTED_HITS, getLineHits(classData, 24))
    })

    @Test
    fun test_IDEA_57695() = test(
        "custom.IDEA_57695",
        configuration = TestConfiguration(hashMapOf(1 to "PARTIAL", 2 to "FULL"), listOf("TestClass"))
    )

    @Test
    fun testInlineMultiplyFiles() {
        val test = getTestFile("custom.inlineMultiplyFiles")
        test(test.testName, test, extractTestConfiguration(File(test.file.parentFile, "test2.kt")))
    }

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

    @Test
    fun testUnloadedMultiFile() {
        val test = getTestFile("custom.unloaded.multiFile")
        test(test.testName, test, extractTestConfiguration(File(test.file.parentFile, "UnusedClass.kt")))
    }

    private fun test_IDEA_275520(projectData: ProjectData, classes: Map<String, Map<String, IntRange>> = hashMapOf(
        "$TEST_PACKAGE.custom.IDEA_275520.Test2Kt" to hashMapOf(
            "simpleInline(I)V" to 20..21,
            "nestedInlines(I)V" to 24..29,
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
}

internal abstract class CoverageVerifyResultsTest(override val coverage: Coverage) : CoverageRunTest() {
    override fun verifyResults(projectData: ProjectData, configuration: TestConfiguration, testFile: File) {
        assertEqualsLines(projectData, configuration.coverageData, configuration.classes)
    }
}

internal abstract class AbstractSamplingCoverageTest(coverage: Coverage) : CoverageVerifyResultsTest(coverage) {
    override fun preprocessConfiguration(configuration: TestConfiguration) =
        configuration.copy(coverageData = configuration.coverageData.mapValues { (_, v) -> if (v == "PARTIAL") "FULL" else v })
}

internal class SamplingCoverageTest : AbstractSamplingCoverageTest(Coverage.SAMPLING)
internal class NewSamplingCoverageTest : AbstractSamplingCoverageTest(Coverage.NEW_SAMPLING)
internal class TracingCoverageTest : CoverageVerifyResultsTest(Coverage.TRACING)
internal class NewTracingCoverageTest : CoverageVerifyResultsTest(Coverage.NEW_TRACING)
