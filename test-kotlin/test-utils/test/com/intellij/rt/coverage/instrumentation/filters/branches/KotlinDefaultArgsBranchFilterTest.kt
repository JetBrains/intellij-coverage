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
package com.intellij.rt.coverage.instrumentation.filters.branches

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.ceil

class KotlinDefaultArgsBranchFilterTest {
    private fun modifiedParametersCount(k: Int) = k + 1 + ceil(k / 32.0).toInt()

    @Test
    fun sourceParametersCount() {
        for (expectedParamCount in 1 until 1e6.toInt()) {
            val modifiedParamCount = modifiedParametersCount(expectedParamCount)
            val actualParamCount = KotlinDefaultArgsBranchFilter.sourceParametersCount(modifiedParamCount)
            assertEquals(expectedParamCount, actualParamCount)
        }
    }
}
