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
package com.intellij.rt.coverage.aggregate

import org.junit.Assert
import org.junit.Test
import java.util.regex.Pattern

val INHERITS = hashMapOf(
    "IB" to arrayOf("IA"),
    "IC" to arrayOf("IA"),
    "B" to arrayOf("IB"),
    "C" to arrayOf("IC"),
    "BC" to arrayOf("IB", "IC"),
    "D" to arrayOf("ID"),
    "D1" to arrayOf("D", "IF"),
    "E" to arrayOf("O"),
)

val ALL_CLASSES: Set<String> = INHERITS.keys + INHERITS.values.flatMap { it.toList() }

class InheritanceFilterTest {
    @Test
    fun `test include 1`() = doTest("B, BC, C, IB, IC", Pattern.compile("IA"))

    @Test
    fun `test include 2`() = doTest("E", Pattern.compile("O"))

    @Test
    fun `test include 3`() = doTest("B, BC, C", Pattern.compile("I[BC]"))

    @Test
    fun `test include and exclude partly`() = doTest("B, IB, IC", Pattern.compile("IA"), Pattern.compile("IC"))

    @Test
    fun `test include overlap`() = doTest("B, BC, C, IC", listOf(Pattern.compile("IA"), Pattern.compile("IB")), emptyList())

    @Test
    fun `test exclude 1`() = doTest("B, D, D1, E, IA, IB, IC, ID, IF, O", excluded = Pattern.compile("IC"))

    @Test
    fun `test exclude 2`() = doTest("B, BC, C, D, D1, E, IA, IB, IC, ID, IF, O", excluded = Pattern.compile("B"))

    @Test
    fun `test exclude 3`() = doTest("D, D1, E, IA, ID, IF, O", excluded = Pattern.compile("IA"))


    private fun doTest(expected: String, included: Pattern? = null, excluded: Pattern? = null) =
        doTest(expected, included.asList(), excluded.asList())

    private fun doTest(expected: String, included: List<Pattern>, excluded: List<Pattern>) {
        val filter = InheritanceFilter(INHERITS)
        val filtered = filter.filterInherits(ALL_CLASSES, included, excluded)
        Assert.assertEquals(expected, filtered.toSortedSet().joinToString())
    }

}

private fun <T> T?.asList() = this?.let { listOf(it) } ?: emptyList()
