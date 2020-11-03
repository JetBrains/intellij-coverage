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


import com.intellij.rt.coverage.assertEqualsLines
import com.intellij.rt.coverage.extractCoverageDataFromFile
import com.intellij.rt.coverage.pathToFile
import com.intellij.rt.coverage.runWithCoverage
import org.junit.After
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
    fun testDefaultArgsCovered() = test("defaultArgs.covered")

    @Test
    fun testDefaultArgsUncovered() = test("defaultArgs.uncovered")

    @Test
    fun testDefaultArgsSeveralArgs() = test("defaultArgs.severalArguments")

    @Test
    @Ignore("Not implemented")
    fun testInline() = test("inline")

    @Test
    @Ignore("Not implemented")
    fun testInlineInline() = test("inlineInline")

    @Test
    @Ignore("Not implemented")
    fun testReturn() = test("returnTest")

    @Test
    fun testFileProperties() = test("properties.file")

    @Test
    @Ignore("Not implemented")
    fun testGetterAndSetterOfPropertyAreDistinguishable() = test("properties.getter_setter")

    @Test
    fun testPrimaryConstructorWithProperties() = test("properties.constructor", "kotlinTestData.properties.constructor.A")

    @Test
    fun testDataClass() = test("dataClass", "kotlinTestData.dataClass.A")

    @Test
    fun testDefaultInterfaceMember() = test("defaultInterfaceMember", "kotlinTestData.defaultInterfaceMember.Foo\$DefaultImpls", "kotlinTestData.defaultInterfaceMember.Bar")

    @Test
    fun testDefaultInterfaceMemberRemoveOnlyInterfaceMember() = test("defaultInterfaceMember.removeOnlyDefaultInterfaceMember", "kotlinTestData.defaultInterfaceMember.removeOnlyDefaultInterfaceMember.Bar")

    @Test
    fun testDefaultInterfaceMemberJava() = test("defaultInterfaceMember.java",
            "kotlinTestData.defaultInterfaceMember.java.Foo", "kotlinTestData.defaultInterfaceMember.java.Bar",
            fileName = "Test.java")

    @Test
    fun testImplementationByDelegation() = test("implementationByDelegation", "kotlinTestData.implementationByDelegation.Derived")

    @Test
    fun testImplementationByDelegationGeneric() = test("implementationByDelegationGeneric", "kotlinTestData.implementationByDelegationGeneric.BDelegation")

    @Test
    fun testJavaSwitch() = test("javaSwitch", "kotlinTestData.javaSwitch.JavaSwitchTest", sampling = false, fileName = "JavaSwitchTest.java")

    @Test
    fun testSealedClassConstructor() = test("sealedClassConstructor",
            "kotlinTestData.sealedClassConstructor.SealedClass",
            "kotlinTestData.sealedClassConstructor.SealedClassWithArgs",
            "kotlinTestData.sealedClassConstructor.ClassWithPrivateDefaultConstructor")


    private fun test(testName: String, vararg classes: String = arrayOf("kotlinTestData.$testName.TestKt"),
                     sampling: Boolean = true, fileName: String = "test.kt") {
        val testFile = pathToFile("src", "kotlinTestData", *testName.split('.').toTypedArray(), fileName)
        val expected = extractCoverageDataFromFile(testFile)
        val project = runWithCoverage(myDataFile, testName, sampling)
        assertEqualsLines(project, expected, classes.toList())
    }
}
