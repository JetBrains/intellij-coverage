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

package com.intellij.rt.coverage.kotlin

import com.intellij.rt.coverage.Coverage
import org.junit.Test

internal abstract class KotlinAbstractTestTrackingTest : KotlinCoverageStatusTest() {
    override val testTracking = true

    @Test
    fun testOneTest() = test("testTracking.oneTest")

    @Test
    fun testTwoTests() = test("testTracking.twoTests")
}

internal class KotlinTestTrackingTracingTest : KotlinAbstractTestTrackingTest() {
    override val coverage = Coverage.TRACING
}

internal class KotlinTestTrackingNewTracingTest : KotlinAbstractTestTrackingTest() {
    override val coverage = Coverage.NEW_TRACING
}
