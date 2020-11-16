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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.io.File


class KotlinCoverageStatusTest {
    private lateinit var myDataFile: File

    @Before
    fun setUp() {
        myDataFile = createTempFile("test")
    }

    @After
    fun tearDown() {
        myDataFile.delete()
    }

    @Test
    fun testSimpleIfElse() = test("simple.ifelse", sampling = false)

    @Test
    fun testDefaultArgsCovered() = test("defaultArgs.covered")

    @Test
    fun testDefaultArgsUncovered() = test("defaultArgs.uncovered")

    @Test
    fun testDefaultArgsSeveralArgs() = test("defaultArgs.severalArguments")

    @Test
    fun testDefaultArgsSeveralArgsTracing() = test("defaultArgs.tracing", "TestKt", "X", sampling = false)

    @Test
    fun test32DefaultArgsTracing() = test("defaultArgs.defaultArgs32", sampling = false)

    @Test
    fun test32ArgsTracing() = test("defaultArgs.args32", sampling = false)

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
    fun testMultiplyFilesInline() = test("inline.multiplyFiles", "Test2Kt",
            fileName = "test2.kt")

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
    fun testDefaultInterfaceMemberJava() = test("defaultInterfaceMember.java",
            "Foo", "Bar",
            fileName = "Test.java")

    @Test
    fun testImplementationByDelegation() = test("implementationByDelegation", "Derived")

    @Test
    fun testImplementationByDelegationGeneric() = test("implementationByDelegationGeneric", "BDelegation")

    @Test
    fun testWhenMappingsSampling() = test("whenMapping.sampling")

    @Test
    fun testWhenMappingTracing() = test("whenMapping.tracing", sampling = false)

    @Test
    fun testJavaSwitch() = test("javaSwitch", "JavaSwitchTest", sampling = false, fileName = "JavaSwitchTest.java")

    @Test
    fun testSealedClassConstructor() = test("sealedClassConstructor",
            "SealedClass", "SealedClassWithArgs", "ClassWithPrivateDefaultConstructor")

    @Test
    fun testFunInterface() = test("funInterface", "TestKt", "TestKt\$test\$1")

    @Test
    fun test_IDEA_57695() {
        val project = runWithCoverage(myDataFile, "fixes.IDEA_57695", false)
        assertEqualsLines(project, hashMapOf(1 to "PARTIAL", 2 to "FULL"), listOf("kotlinTestData.fixes.IDEA_57695.TestClass"))
    }

    @Test
    fun test_IDEA_250825() = test("fixes.IDEA_250825", "JavaTest", fileName = "JavaTest.java", sampling = false)

    @Test
    fun testThreadSafeStructure() {
        val n = THREAD_SAFE_STRUCTURE_CLASSES
        val expected = (1..n).associateWith { "FULL" }
        val project = runWithCoverage(myDataFile, "threadSafe.structure", false)
        assertEqualsLines(project, expected, (0 until n).map { "kotlinTestData.threadSafe.structure.Class$it" })
    }

    @Test
    @Ignore("Coverage hit increment is not atomic.")
    fun testThreadSafeData() {
        val project = runWithCoverage(myDataFile, "threadSafe.data", false)
        val data = project.getClassData("kotlinTestData.threadSafe.data.SimpleClass")
        assertEquals(THREAD_SAFE_DATA_EXPECTED_HITS, getLineHits(data, 24))
    }

    private fun test(testName: String, vararg classes: String = arrayOf("TestKt"),
                     sampling: Boolean = true, fileName: String = "test.kt",
                     calcUnloaded: Boolean = false) {
        val testFile = pathToFile("src", "kotlinTestData", *testName.split('.').toTypedArray(), fileName)
        val expected = extractCoverageDataFromFile(testFile)
        val project = runWithCoverage(myDataFile, testName, sampling, calcUnloaded)
        val fullClassNames = classes.map { "kotlinTestData.$testName.$it" }
        assertEqualsLines(project, expected, if (classes.contains(all)) listOf(all) else fullClassNames)
    }
}
