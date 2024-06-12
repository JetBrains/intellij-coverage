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

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import testData.custom.threadSafe.structure.THREAD_SAFE_STRUCTURE_CLASSES
import java.io.File
import kotlin.test.*

/**
 * Tests between ===GENERATED TESTS=== marker are generated automatically by testGeneration.kt code.
 * @see [testGeneration.kt]
 */
@RunWith(Parameterized::class)
internal class CoverageRunTest(override val coverage: Coverage, override val testTracking: TestTracking?) :
    CoverageTest() {
    companion object {
        /**
         * We run these tests with test tracking to test the test tracking instrumentation issues,
         * while the test tracking logic is tested in [com.intellij.rt.coverage.caseTests.TestTrackingTest].
         */
        @JvmStatic
        @Parameterized.Parameters(name = "{0} coverage with {1} test tracking")
        fun data() = getCoverageConfigurations()
    }

    //===GENERATED TESTS===

    @Test
    fun testBasicJavaAssertion() = test("basicJava.assertion")

    @Test
    fun testBasicJavaBadCycleClasses() = test("basicJava.badCycle.classes")

    @Test
    fun testBasicJavaBadCycleInterfaces() = test("basicJava.badCycle.interfaces")

    @Test
    fun testBasicJavaDeserializeLambda() = test("basicJava.deserializeLambda")

    @Test
    fun testBasicJavaInterfaces() = test("basicJava.interfaces")

    @Test
    fun testBasicJavaLombok() = test("basicJava.lombok")

    @Test
    fun testCasesElseif() = test("cases.elseif")

    @Test
    fun testCasesFallthrough() = test("cases.fallthrough")

    @Test
    fun testCasesIfelse() = test("cases.ifelse")

    @Test
    fun testCasesIntMaxSwitch() = test("cases.intMaxSwitch")

    @Test
    fun testCasesJavaIf() = test("cases.javaIf")

    @Test
    fun testCasesJavaSwitch() = test("cases.javaSwitch")

    @Test
    fun testCasesWhenBoolean() = test("cases.whenBoolean")

    @Test
    fun testCasesWhenEnum() = test("cases.whenEnum")

    @Test
    fun testCasesWhenString() = test("cases.whenString")

    @Test
    fun testCoroutinesAsync() = test("coroutines.async")

    @Test
    fun testCoroutinesCrossinline() = test("coroutines.crossinline")

    @Test
    fun testCoroutinesCrossinlineCall() = test("coroutines.crossinlineCall")

    @Test
    fun testCoroutinesDefaultArgs() = test("coroutines.defaultArgs")

    @Test
    fun testCoroutinesFunction() = test("coroutines.function")

    @Test
    fun testCoroutinesInline() = test("coroutines.inline")

    @Test
    fun testCoroutinesJumpAfterCondition() = test("coroutines.jumpAfterCondition")

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
    fun testCoverageFeaturesIgnoreAnnotationAnonInLocalFun() = test("coverageFeatures.ignoreAnnotation.anonInLocalFun")

    @Test
    fun testCoverageFeaturesIgnoreAnnotationAnonymous() = test("coverageFeatures.ignoreAnnotation.anonymous")

    @Test
    fun testCoverageFeaturesIgnoreAnnotationDefaultArgs() = test("coverageFeatures.ignoreAnnotation.defaultArgs")

    @Test
    fun testCoverageFeaturesIgnoreAnnotationDeprecated() = test("coverageFeatures.ignoreAnnotation.deprecated")

    @Test
    fun testCoverageFeaturesIgnoreAnnotationFullClass() = test("coverageFeatures.ignoreAnnotation.fullClass")

    @Test
    fun testCoverageFeaturesIgnoreAnnotationIncludeAnnotationConflicting() = test("coverageFeatures.ignoreAnnotation.includeAnnotation.conflicting")

    @Test
    fun testCoverageFeaturesIgnoreAnnotationIncludeAnnotationFullClass() = test("coverageFeatures.ignoreAnnotation.includeAnnotation.fullClass")

    @Test
    fun testCoverageFeaturesIgnoreAnnotationIncludeAnnotationMethod() = test("coverageFeatures.ignoreAnnotation.includeAnnotation.method")

    @Test
    fun testCoverageFeaturesIgnoreAnnotationInline() = test("coverageFeatures.ignoreAnnotation.inline")

    @Test
    fun testCoverageFeaturesIgnoreAnnotationInlineUnloaded() = test("coverageFeatures.ignoreAnnotation.inlineUnloaded")

    @Test
    fun testCoverageFeaturesIgnoreAnnotationInlined() = test("coverageFeatures.ignoreAnnotation.inlined")

    @Test
    fun testCoverageFeaturesIgnoreAnnotationMethod() = test("coverageFeatures.ignoreAnnotation.method")

    @Test
    fun testCoverageFeaturesNotJava() = test("coverageFeatures.not.java")

    @Test
    fun testCoverageFeaturesNotKotlin() = test("coverageFeatures.not.kotlin")

    @Test
    fun testCoverageFeaturesRedefine() = test("coverageFeatures.redefine")

    @Test
    fun testCoverageFeaturesReturnTest() = test("coverageFeatures.returnTest")

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
    fun testEnum_Java() = test("enum_.java")

    @Test
    fun testEnum_Kotlin() = test("enum_.kotlin")

    @Test
    fun testFixesIDEA_323017() = test("fixes.IDEA_323017")

    @Test
    fun testFixesClassReload() = test("fixes.classReload")

    @Test
    fun testFixesLineIgnore() = test("fixes.lineIgnore")

    @Test
    fun testInlineCoroutines() = test("inline.coroutines")

    @Test
    fun testInlineCrossinline() = test("inline.crossinline")

    @Test
    fun testInlineFilteringExclude() = test("inline.filtering.exclude")

    @Test
    fun testInlineFilteringNoInclude() = test("inline.filtering.noInclude")

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
    fun testKotlinFeaturesDataClass() = test("kotlinFeatures.dataClass")

    @Test
    fun testKotlinFeaturesDefaultInterfaceMemberJava() = test("kotlinFeatures.defaultInterfaceMember.java")

    @Test
    fun testKotlinFeaturesDefaultInterfaceMemberKotlin() = test("kotlinFeatures.defaultInterfaceMember.kotlin")

    @Test
    fun testKotlinFeaturesDefaultInterfaceMemberRemoveOnlyDefaultInterfaceMember() = test("kotlinFeatures.defaultInterfaceMember.removeOnlyDefaultInterfaceMember")

    @Test
    fun testKotlinFeaturesDefaultInterfaceMemberWithArgs() = test("kotlinFeatures.defaultInterfaceMember.withArgs")

    @Test
    fun testKotlinFeaturesFunInterface() = test("kotlinFeatures.funInterface")

    @Test
    fun testKotlinFeaturesImplementationByDelegationBasic() = test("kotlinFeatures.implementationByDelegation.basic")

    @Test
    fun testKotlinFeaturesImplementationByDelegationGeneric() = test("kotlinFeatures.implementationByDelegation.generic")

    @Test
    fun testKotlinFeaturesInterfaceWithClinit() = test("kotlinFeatures.interfaceWithClinit")

    @Test
    fun testKotlinFeaturesLateinitInternal() = test("kotlinFeatures.lateinit.internal")

    @Test
    fun testKotlinFeaturesLateinitSimple() = test("kotlinFeatures.lateinit.simple")

    @Test
    fun testKotlinFeaturesNullability() = test("kotlinFeatures.nullability")

    @Test
    fun testKotlinFeaturesPropertiesConstructor() = test("kotlinFeatures.properties.constructor")

    @Test
    fun testKotlinFeaturesPropertiesFile() = test("kotlinFeatures.properties.file")

    @Test
    fun testKotlinFeaturesSealedBasic() = test("kotlinFeatures.sealed.basic")

    @Test
    fun testKotlinFeaturesSealedConstructor() = test("kotlinFeatures.sealed.constructor")

    @Test
    fun testKotlinFeaturesTypeCast() = test("kotlinFeatures.typeCast")

    @Test
    fun testKotlinFeaturesValueClass() = test("kotlinFeatures.valueClass")

    @Test
    fun testSimpleBranches() = test("simple.branches")

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
    fun testUtilClassObject() = test("utilClass.object")

    @Test
    fun testUtilClassThrowing() = test("utilClass.throwing")

    //===GENERATED TESTS===

    @Test
    fun testThreadSafeStructure() = test(
        "custom.threadSafe.structure", configuration = TestConfiguration(
            coverageData = (1..THREAD_SAFE_STRUCTURE_CLASSES).associateWith { "FULL" },
            classes = (0 until THREAD_SAFE_STRUCTURE_CLASSES).map { "Class$it" },
        )
    )

    @Test
    fun test_IDEA_57695() = test(
        "custom.IDEA_57695", configuration = TestConfiguration(
            coverageData = hashMapOf(1 to "PARTIAL", 2 to "FULL"),
            classes = listOf("TestClass"),
        )
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
        test(testName) { projectData, config ->
            verifyResults(projectData, config)
            assertTrue { projectData.getClassData("testData.$testName.TestKt").isFullyAnalysed }
            assertFalse { projectData.getClassData("testData.$testName.UnusedClass").isFullyAnalysed }
        }
    }

    @Test
    fun testMethodReference() {
        val testName = "custom.methodReference"
        test(testName) { projectData, config ->
            verifyResults(projectData, config)
            assertEquals(2, projectData.classesNumber)
            assertNotNull(projectData.getClassData("testData.$testName.TestKt"))
            assertNotNull(projectData.getClassData("testData.$testName.Foo"))
        }
    }
}
