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

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests between ===GENERATED TESTS=== marker are generated automatically by testGeneration.kt code.
 */
@RunWith(Parameterized::class)
internal class Jdk11CoverageRunTest(override val coverage: Coverage, override val testTracking: TestTracking?) :
    CoverageTest() {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} coverage with {1} test tracking")
        fun data() = getCoverageConfigurations()
    }

    //===GENERATED TESTS===

    @Test
    fun testBasicJavaTryWithResourcesJava() = test("basicJava.tryWithResources.java")

    @Test
    fun testBasicJavaTryWithResourcesKotlin() = test("basicJava.tryWithResources.kotlin")

    @Test
    fun testComposeBasic() = test("compose.basic")

    @Test
    fun testComposeDefaultArgs() = test("compose.defaultArgs")

    @Test
    fun testComposeSynthetic() = test("compose.synthetic")

    @Test
    fun testComposeText() = test("compose.text")

    //===GENERATED TESTS===
}
