/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

package com.intellij.rt.coverage.kotlin


import com.intellij.rt.coverage.*
import kotlinTestData.threadSafe.data.THREAD_SAFE_DATA_EXPECTED_HITS
import kotlinTestData.threadSafe.structure.THREAD_SAFE_STRUCTURE_CLASSES
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test


abstract class KotlinCoverageStatusAbstractSamplingTest : KotlinCoverageStatusTest() {
    @Test
    fun testDefaultArgsCovered() = test("defaultArgs.covered")

    @Test
    fun testDefaultArgsUncovered() = test("defaultArgs.uncovered")

    @Test
    fun testDefaultArgsSeveralArgs() = test("defaultArgs.severalArguments")

    @Test
    fun testDefaultArgsInline() = test("defaultArgs.inline")

    @Test
    fun testUnloadedSingleFile() = test("unloaded.singleFile", "UnusedClass", calcUnloaded = true)

    @Test
    fun testUnloadedMultiFile() = test("unloaded.multiFile", "UnusedClass", calcUnloaded = true, fileName = "UnusedClass.kt")

    @Test
    fun testSimpleInline() = test("inline.simple")

    @Test
    fun testInlineInline() = test("inline.inlineInline")

    @Test
    fun testLambdaInline() = test("inline.lambda")

    @Test
    fun testReflection() = test("inline.reflection", "Test")

    @Test
    fun testReified() = test("inline.reified")

    @Test
    fun testInlineCoroutines() = test("inline.coroutines.sampling", all)

    @Test
    fun testNoInline() = test("inline.noinline", all)

    @Test
    fun testCrossInline() = test("inline.crossinline", all)

    @Test
    fun testMultiplyFilesInline() = test("inline.multiplyFiles", "Test2Kt", fileName = "test2.kt")

    @Test
    @Ignore("Not implemented")
    fun testReturn() = test("returnTest")

    @Test
    fun testFileProperties() = test("properties.file")

    @Test
    @Ignore("Not implemented")
    fun testGetterAndSetterOfPropertyAreDistinguishable() = test("properties.getter_setter")

    @Test
    fun testPrimaryConstructorWithProperties() = test("properties.constructor", "A")

    @Test
    fun testDataClass() = test("dataClass", "A")

    @Test
    fun testDefaultInterfaceMember() = test("defaultInterfaceMember", "Foo\$DefaultImpls", "Bar")

    @Test
    fun testDefaultInterfaceMemberRemoveOnlyInterfaceMember() = test("defaultInterfaceMember.removeOnlyDefaultInterfaceMember", "Bar")

    @Test
    fun test_IDEA_259731() = test("fixes.IDEA_259731", "C")

    @Test
    fun testDefaultInterfaceMemberJava() = test("defaultInterfaceMember.java", "Foo", "Bar", fileName = "Test.java")

    @Test
    @Ignore("To be fixed")
    fun testCoroutinesFix1() = test("coroutines.fix1.sampling", all)

    @Test
    fun testImplementationByDelegation() = test("implementationByDelegation", "Derived")

    @Test
    fun testImplementationByDelegationGeneric() = test("implementationByDelegationGeneric", "BDelegation")

    @Test
    fun testWhenMappings() = test("whenMapping.sampling")

    @Test
    fun testSealedClassConstructor() = test("sealedClassConstructor",
            "SealedClass", "SealedClassWithArgs", "ClassWithPrivateDefaultConstructor")

    @Test
    fun testFunInterface() = test("funInterface", "TestKt", "TestKt\$test\$1")

    @Test
    fun testThreadSafeStructure() {
        val n = THREAD_SAFE_STRUCTURE_CLASSES
        val expected = (1..n).associateWith { "FULL" }
        val project = runWithCoverage(myDataFile, "threadSafe.structure", coverage)
        assertEqualsLines(project, expected, (0 until n).map { "kotlinTestData.threadSafe.structure.Class$it" })
    }

    @Test
    @Ignore("Coverage hit increment is not atomic.")
    fun testThreadSafeData() {
        val project = runWithCoverage(myDataFile, "threadSafe.data", coverage)
        val data = project.getClassData("kotlinTestData.threadSafe.data.SimpleClass")
        Assert.assertEquals(THREAD_SAFE_DATA_EXPECTED_HITS, getLineHits(data, 24))
    }

    @Test
    fun testBadCycleClasses() = test("badCycle.classes", "JavaTest\$BaseClass", "JavaTest\$DerivedClass", fileName = "JavaTest.java")

    @Test
    fun testBadCycleInterfaces() = test("badCycle.interfaces", "JavaTest\$ImplementerInterface", fileName = "JavaTest.java")

    @Test
    fun test_KT_39038() = test("fixes.KT_39038")
}

class KotlinCoverageStatusSamplingTest : KotlinCoverageStatusAbstractSamplingTest() {
    override val coverage = Coverage.SAMPLING
}

class KotlinCoverageStatusNewSamplingTest : KotlinCoverageStatusAbstractSamplingTest() {
    override val coverage = Coverage.NEW_SAMPLING
}
