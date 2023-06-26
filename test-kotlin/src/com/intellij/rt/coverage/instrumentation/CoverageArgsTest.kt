/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation

import com.intellij.rt.coverage.createTmpFile
import org.junit.Assert
import org.junit.Test
import java.util.regex.Pattern

class CoverageArgsTest {
    @Test
    fun `test minimal args`() {
        val input = "file.ic true true true true"
        val args = CoverageArgs.fromString(input)
        Assert.assertEquals("file.ic", args.dataFile.path)
        Assert.assertTrue(args.testTracking)
        Assert.assertTrue(args.calcUnloaded)
        Assert.assertTrue(args.mergeData)
        Assert.assertFalse(args.branchCoverage)

        Assert.assertEquals(null, args.sourceMap)
        assertEqualsPatterns(listOf(), args.includePatterns)
        assertEqualsPatterns(listOf(), args.includePatterns)
        assertEqualsPatterns(listOf(), args.includePatterns)
    }

    @Test
    fun `test all args from string`() {
        val args = CoverageArgs.fromString(BASIC_INPUT)
        testBasicInput(args)
    }

    @Test
    fun `test all args from file`() {
        val file = createTmpFile("coverage")
        file.writeText(BASIC_INPUT.replace(' ', '\n'))

        val args = CoverageArgs.fromString(file.absolutePath)
        testBasicInput(args)

        file.delete()
    }

    @Test
    fun `test no smap`() {
        val input = "file.ic true true true true include.* -exclude exclude.* -excludeAnnotations annotations.*"
        val args = CoverageArgs.fromString(input)
        Assert.assertEquals("file.ic", args.dataFile.path)
        Assert.assertTrue(args.testTracking)
        Assert.assertTrue(args.calcUnloaded)
        Assert.assertTrue(args.mergeData)
        Assert.assertFalse(args.branchCoverage)

        Assert.assertEquals(null, args.sourceMap)
        assertEqualsPatterns(listOf("include.*"), args.includePatterns)
        assertEqualsPatterns(listOf("exclude.*"), args.excludePatterns)
        assertEqualsPatterns(listOf("annotations.*"), args.annotationsToIgnore)
    }
}

private const val BASIC_INPUT =
    "file.ic true true true true true file.smap include.* include2.* -exclude exclude.* -excludeAnnotations annotations.*"

private fun testBasicInput(args: CoverageArgs) {
    Assert.assertEquals("file.ic", args.dataFile.path)
    Assert.assertTrue(args.testTracking)
    Assert.assertTrue(args.calcUnloaded)
    Assert.assertTrue(args.mergeData)
    Assert.assertFalse(args.branchCoverage)

    Assert.assertEquals("file.smap", args.sourceMap.path)
    assertEqualsPatterns(listOf("include.*", "include2.*"), args.includePatterns)
    assertEqualsPatterns(listOf("exclude.*"), args.excludePatterns)
    assertEqualsPatterns(listOf("annotations.*"), args.annotationsToIgnore)
}

private fun assertEqualsPatterns(expected: List<String>, actual: List<Pattern>) {
    Assert.assertEquals(expected, actual.map { it.pattern() })
}
