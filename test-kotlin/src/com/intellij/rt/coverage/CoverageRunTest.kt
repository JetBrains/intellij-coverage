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
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import testData.custom.threadSafe.data.THREAD_SAFE_DATA_EXPECTED_HITS
import testData.custom.threadSafe.structure.THREAD_SAFE_STRUCTURE_CLASSES
import java.io.File
import kotlin.test.*

/**
 * Tests between ===GENERATED TESTS=== marker are generated automatically by testGeneration.kt code.
 * @see [generateTests]
 */
@RunWith(Parameterized::class)
internal class CoverageRunTest(override val coverage: Coverage, override val testTracking: TestTracking?) : CoverageTest() {
    companion object {
        /**
         * We run these tests with test tracking to test the test tracking instrumentation issues,
         * while the test tracking logic is tested in [com.intellij.rt.coverage.caseTests.TestTrackingTest].
         */
        @JvmStatic
        @Parameterized.Parameters(name = "{0} coverage with {1} test tracking")
        fun data() = if (System.getProperty("coverage.run.fast.tests") != null) {
            arrayOf(
                arrayOf(Coverage.BRANCH_FIELD, null),
                arrayOf(Coverage.LINE_CONDY, null)
            )
        } else {
            allTestTrackingModes()
        }
    }

    override fun preprocessConfiguration(configuration: TestConfiguration) = if (coverage.isBranchCoverage()) {
        super.preprocessConfiguration(configuration)
    } else {
        configuration.copy(coverageData = configuration.coverageData.mapValues { (_, v) -> if (v == "PARTIAL") "FULL" else v })
    }

    override fun verifyResults(projectData: ProjectData, configuration: TestConfiguration, testFile: File) {
        if (configuration.expectedClasses != null) {
            val actualClasses = projectData.classesCollection.map { it.name }
            val expectedClasses = configuration.expectedClasses
            assertEquals(expectedClasses.sorted(), actualClasses.sorted())
        }
        assertEqualsLines(projectData, configuration.coverageData, configuration.classes)
    }

    //===GENERATED TESTS===

    @Test
    fun testBadCycleClasses() = test("badCycle.classes")

    @Test
    fun testBadCycleInterfaces() = test("badCycle.interfaces")

    @Test
    fun testCasesJavaSwitch() = test("cases.javaSwitch")

    @Test
    fun testCasesWhenEnum() = test("cases.whenEnum")

    @Test
    fun testCasesWhenString() = test("cases.whenString")

    @Test
    fun testCoroutinesAsync() = test("coroutines.async")

    @Test
    fun testCoroutinesCrossinline() = test("coroutines.crossinline")

    @Test
    fun testCoroutinesDefaultArgs() = test("coroutines.defaultArgs")

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
    fun testCoroutinesSmallBody() = test("coroutines.smallBody")

    @Test
    fun testCoroutinesSuspendLambda() = test("coroutines.suspendLambda")

    @Test
    fun testCoroutinesTailSuspendCall() = test("coroutines.tailSuspendCall")

    @Test
    fun testDataClass() = test("dataClass")

    @Test
    fun testDefaultArgsArgs32() = test("defaultArgs.args32")

    @Test
    fun testDefaultArgsConstructor() = test("defaultArgs.constructor")

    @Test
    fun testDefaultArgsCovered() = test("defaultArgs.covered")

    @Test
    fun testDefaultArgsDefaultArgs32() = test("defaultArgs.defaultArgs32")

    @Test
    fun testDefaultArgsInline() = test("defaultArgs.inline")

    @Test
    fun testDefaultArgsOpenMethod() = test("defaultArgs.openMethod")

    @Test
    fun testDefaultArgsSeveralArguments() = test("defaultArgs.severalArguments")

    @Test
    fun testDefaultArgsSimple() = test("defaultArgs.simple")

    @Test
    fun testDefaultArgsUncovered() = test("defaultArgs.uncovered")

    @Test
    fun testDefaultInterfaceMemberJava() = test("defaultInterfaceMember.java")

    @Test
    fun testDefaultInterfaceMemberKotlin() = test("defaultInterfaceMember.kotlin")

    @Test
    fun testDefaultInterfaceMemberRemoveOnlyDefaultInterfaceMember() = test("defaultInterfaceMember.removeOnlyDefaultInterfaceMember")

    @Test
    fun testDeprecated() = test("deprecated")

    @Test
    fun testDeserializeLambda() = test("deserializeLambda")

    @Test
    fun testEnum_Java() = test("enum_.java")

    @Test
    fun testEnum_Kotlin() = test("enum_.kotlin")

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
    fun testFixesIDEA_295404() = test("fixes.IDEA_295404")

    @Test
    fun testFixesIDEA_323017() = test("fixes.IDEA_323017")

    @Test
    fun testFixesKT_39038() = test("fixes.KT_39038")

    @Test
    fun testFixesIntMaxSwitch() = test("fixes.intMaxSwitch")

    @Test
    fun testFunInterface() = test("funInterface")

    @Test
    fun testIgnoreAnnotationAnonInLocalFun() = test("ignoreAnnotation.anonInLocalFun")

    @Test
    fun testIgnoreAnnotationAnonymous() = test("ignoreAnnotation.anonymous")

    @Test
    fun testIgnoreAnnotationDefaultArgs() = test("ignoreAnnotation.defaultArgs")

    @Test
    fun testIgnoreAnnotationFullClass() = test("ignoreAnnotation.fullClass")

    @Test
    fun testIgnoreAnnotationInline() = test("ignoreAnnotation.inline")

    @Test
    fun testIgnoreAnnotationInlineUnloaded() = test("ignoreAnnotation.inlineUnloaded")

    @Test
    fun testIgnoreAnnotationInlined() = test("ignoreAnnotation.inlined")

    @Test
    fun testIgnoreAnnotationMethod() = test("ignoreAnnotation.method")

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
    fun testInlineMultiplyFiles() = test("inline.multiplyFiles")

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
    fun testInterfaces() = test("interfaces")

    @Test
    fun testLateinitInternal() = test("lateinit.internal")

    @Test
    fun testLateinitSimple() = test("lateinit.simple")

    @Test
    fun testLombok() = test("lombok")

    @Test
    fun testNotJava() = test("not.java")

    @Test
    fun testNotKotlin() = test("not.kotlin")

    @Test
    fun testNullability() = test("nullability")

    @Test
    fun testObject() = test("object")

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
    fun testSealed() = test("sealed")

    @Test
    fun testSealedClassConstructor() = test("sealedClassConstructor")

    @Test
    fun testSimpleBranches() = test("simple.branches")

    @Test
    fun testSimpleIfelse() = test("simple.ifelse")

    @Test
    fun testTypeCast() = test("typeCast")

    @Test
    fun testUnloadedCycle() = test("unloaded.cycle")

    @Test
    fun testUnloadedInline() = test("unloaded.inline")

    @Test
    fun testUnloadedMultiFile() = test("unloaded.multiFile")

    @Test
    fun testUnloadedOuter() = test("unloaded.outer")

    @Test
    fun testUnloadedSingleFile() = test("unloaded.singleFile")

    @Test
    fun testUtilClassJava() = test("utilClass.java")

    @Test
    fun testUtilClassKotlin() = test("utilClass.kotlin")

    @Test
    fun testUtilClassThrowing() = test("utilClass.throwing")

    @Test
    fun testValueClass() = test("valueClass")

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
    fun testIDEA_281195_JMochit() {
        val test = getTestFile("custom.IDEA_281195")
        val configuration = extractTestConfiguration(test.file)
        val jmochitAgent = System.getProperty("java.class.path").split(File.pathSeparator).single { it.contains("jmockit") }
        configuration.extraArgs.add("-javaagent:$jmochitAgent")
        test(test.testName, test, configuration)
    }

    @Test
    fun testReflection() {
        if (coverage == Coverage.LINE_FIELD || coverage == Coverage.BRANCH_FIELD) {
            Assert.assertThrows(RuntimeException::class.java) {
                test("custom.reflection")
            }
        } else {
            test("custom.reflection")
        }
    }

    @Test
    fun testIDEA_299956() {
        val testName = "custom.IDEA_299956"
        test(testName) { projectData, config, file ->
            verifyResults(projectData, config, file)
            assertTrue { projectData.getClassData("testData.$testName.TestKt").isFullyAnalysed }
            assertFalse { projectData.getClassData("testData.$testName.UnusedClass").isFullyAnalysed }
        }
    }

    @Test
    fun testMethodReference() {
        val testName = "custom.methodReference"
        test(testName) { projectData, config, file ->
            verifyResults(projectData, config, file)
            assertEquals(2, projectData.classesNumber)
            assertNotNull(projectData.getClassData("testData.$testName.TestKt"))
            assertNotNull(projectData.getClassData("testData.$testName.Foo"))
        }
    }
}

internal fun allTestTrackingModes() = Coverage.valuesWithCondyWhenPossible().toList()
    .product(TestTracking.values().toList().plus(null))
    .map { it.toList().toTypedArray() }.toTypedArray()
