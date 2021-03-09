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


import com.intellij.rt.coverage.Coverage
import com.intellij.rt.coverage.all
import com.intellij.rt.coverage.assertEqualsLines
import com.intellij.rt.coverage.runWithCoverage
import org.junit.Ignore
import org.junit.Test


class KotlinCoverageStatusTracingTest : KotlinCoverageStatusTest() {
    override val coverage = Coverage.TRACING

    @Test
    fun testSimpleIfElse() = test("simple.ifelse")

    @Test
    fun testDefaultArgsSeveralArgs() = test("defaultArgs.tracing", "TestKt", "X")

    @Test
    fun test32DefaultArgs() = test("defaultArgs.defaultArgs32")

    @Test
    fun test32Args() = test("defaultArgs.args32")

    @Test
    fun testDefaultArgsInline() = test("defaultArgs.inline")

    @Test
    @Ignore("To be fixed")
    fun testInlineCoroutines() = test("inline.coroutines.tracing", all)

    @Test
    fun testCoroutinesLambda() = test("coroutines.lambda", "TestKt", "TestKt\$test\$1")

    @Test
    fun testCoroutinesFunction() = test("coroutines.function", "TestKt", "TestKt\$test\$1")

    @Test
    fun testCoroutinesTailSuspend() = test("coroutines.tailSuspendCall")

    @Test
    fun testCoroutinesNoSuspend() = test("coroutines.noSuspend")

    @Test
    fun testCoroutinesNonVoid() = test("coroutines.nonVoid", "TestKt", "TestKt\$test\$1")

    @Test
    fun testCoroutinesAsync() = test("coroutines.async", all)

    @Test
    fun testCoroutinesInline() = test("coroutines.inline", "TestKt\$test\$1", "TestKt")

    @Test
    @Ignore("To be fixed")
    fun testCoroutinesFix1() = test("coroutines.fix1.tracing", all)

    @Test
    fun testWhenMapping() = test("whenMapping.tracing")

    @Test
    fun testJavaSwitch() = test("javaSwitch", "JavaSwitchTest", fileName = "JavaSwitchTest.java")

    @Test
    fun test_IDEA_57695() {
        val project = runWithCoverage(myDataFile, "fixes.IDEA_57695", coverage)
        assertEqualsLines(project, hashMapOf(1 to "PARTIAL", 2 to "FULL"), listOf("kotlinTestData.fixes.IDEA_57695.TestClass"))
    }

    @Test
    fun test_IDEA_250825() = test("fixes.IDEA_250825", "JavaTest", fileName = "JavaTest.java")

    @Test
    fun test_IDEA_259332() = test("fixes.IDEA_259332", "SwitchWithFallthrough", fileName = "SwitchWithFallthrough.java")
}
