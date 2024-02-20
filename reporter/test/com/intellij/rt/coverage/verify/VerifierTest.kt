/*
 * Copyright 2000-2022 JetBrains s.r.o.
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
package com.intellij.rt.coverage.verify

import com.intellij.rt.coverage.aggregate.AggregatorTest
import com.intellij.rt.coverage.aggregate.api.Request
import com.intellij.rt.coverage.report.TestUtils.checkLogFile
import com.intellij.rt.coverage.report.TestUtils.clearLogFile
import com.intellij.rt.coverage.report.TestUtils.createFilters
import com.intellij.rt.coverage.verify.api.*
import com.intellij.rt.coverage.verify.api.Target
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.util.*
import java.util.regex.Pattern

class VerifierTest {
    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun test1() {
        val rules: MutableList<Rule> = ArrayList()
        val bound1_1 = Bound(1, Counter.LINE, ValueType.COVERED, BigDecimal.valueOf(10), BigDecimal.valueOf(15))
        val bound1_2 = Bound(2, Counter.BRANCH, ValueType.COVERED_RATE, BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.9))
        rules.add(createRule(Target.ALL, bound1_1, bound1_2))

        val bound2_1 = Bound(1, Counter.INSTRUCTION, ValueType.COVERED, BigDecimal.valueOf(10), BigDecimal.valueOf(15))
        val bound2_2 = Bound(2, Counter.BRANCH, ValueType.MISSED_RATE, BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.9))
        rules.add(createRule(Target.CLASS, bound2_1, bound2_2))

        val bound3_1 = Bound(1, Counter.LINE, ValueType.MISSED, BigDecimal.valueOf(10), BigDecimal.valueOf(15))
        val bound3_2 =
            Bound(2, Counter.BRANCH, ValueType.COVERED_RATE, BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.9))
        rules.add(createRule(Target.PACKAGE, bound3_1, bound3_2))


        val boundViolation1x2 = BoundViolation(2)
        boundViolation1x2.minViolations.add(Violation("all", BigDecimal("0.090909")))
        val rule1 = RuleViolation(1, listOf(boundViolation1x2))

        val boundViolation2x1 = BoundViolation(1)
        boundViolation2x1.minViolations.add(
            Violation(
                "testData.noReport.branches.MyBranchedUnloadedClass",
                BigDecimal("0")
            )
        )
        boundViolation2x1.minViolations.add(Violation("TestTopLevelKt", BigDecimal("2")))
        boundViolation2x1.minViolations.add(Violation("testData.branches.MyBranchedClass", BigDecimal("9")))
        boundViolation2x1.minViolations.add(Violation("testData.branches.MyBranchedUnloadedClass", BigDecimal("0")))
        boundViolation2x1.minViolations.add(Violation("testData.noReport.branches.MyBranchedClass", BigDecimal("0")))
        boundViolation2x1.minViolations.add(
            Violation(
                "testData.crossinline.TestKt\$main$\$inlined\$run$1",
                BigDecimal("0")
            )
        )
        boundViolation2x1.minViolations.add(Violation("testData.crossinline.TestKt\$run$2", BigDecimal("0")))
        boundViolation2x1.minViolations.add(Violation("testData.noReport.branches.TestKt", BigDecimal("0")))
        boundViolation2x1.minViolations.add(Violation("testData.defaultArgs.Example", BigDecimal("0")))
        boundViolation2x1.minViolations.add(Violation("testData.crossinline.TestKt\$main$3", BigDecimal("0")))
        boundViolation2x1.minViolations.add(Violation("testData.defaultArgs.TestKt", BigDecimal("0")))
        boundViolation2x1.minViolations.add(
            Violation(
                "testData.outOfPackageStructure.TestOutOfPackageStructureKt",
                BigDecimal("0")
            )
        )
        boundViolation2x1.minViolations.add(Violation("testData.branches.TestKt", BigDecimal("5")))
        boundViolation2x1.minViolations.add(Violation("testData.crossinline.TestKt", BigDecimal("0")))

        boundViolation2x1.maxViolations.add(Violation("testData.simple.Main", BigDecimal("16")))
        boundViolation2x1.maxViolations.add(Violation("testData.inline.TestKt", BigDecimal("18")))

        val boundViolation2x2 = BoundViolation(2)
        boundViolation2x2.maxViolations.add(
            Violation(
                "testData.noReport.branches.MyBranchedUnloadedClass",
                BigDecimal("1.000000")
            )
        )
        boundViolation2x2.maxViolations.add(
            Violation(
                "testData.branches.MyBranchedUnloadedClass",
                BigDecimal("1.000000")
            )
        )
        boundViolation2x2.maxViolations.add(
            Violation(
                "testData.noReport.branches.MyBranchedClass",
                BigDecimal("1.000000")
            )
        )

        val rule2 = RuleViolation(2, Arrays.asList(boundViolation2x1, boundViolation2x2))

        val boundViolation3x1 = BoundViolation(1)
        boundViolation3x1.minViolations.add(Violation("", BigDecimal.ZERO))
        boundViolation3x1.minViolations.add(Violation("testData.inline", BigDecimal("2")))
        boundViolation3x1.minViolations.add(Violation("testData.defaultArgs", BigDecimal("5")))
        boundViolation3x1.minViolations.add(Violation("testData.outOfPackageStructure", BigDecimal("1")))
        boundViolation3x1.minViolations.add(Violation("testData.simple", BigDecimal("4")))
        boundViolation3x1.minViolations.add(Violation("testData.crossinline", BigDecimal("4")))

        boundViolation3x1.maxViolations.add(Violation("testData.noReport.branches", BigDecimal("23")))
        boundViolation3x1.maxViolations.add(Violation("testData.branches", BigDecimal("19")))

        val boundViolation3x2 = BoundViolation(2)
        boundViolation3x2.minViolations.add(Violation("testData.noReport.branches", BigDecimal("0.000000")))
        boundViolation3x2.minViolations.add(Violation("testData.branches", BigDecimal("0.071429")))

        val rule3 = RuleViolation(3, Arrays.asList(boundViolation3x1, boundViolation3x2))

        runVerifier(rules, Arrays.asList(rule1, rule2, rule3))
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun test2() {
        val rules: MutableList<Rule> = ArrayList()
        val bound1_1 = Bound(1, Counter.LINE, ValueType.COVERED, BigDecimal.valueOf(10), BigDecimal.valueOf(15))
        val bound1_2 = Bound(2, Counter.BRANCH, ValueType.COVERED_RATE, BigDecimal.valueOf(0.0), null)
        rules.add(createRule(Target.ALL, bound1_1, bound1_2))

        val bound2_1 = Bound(1, Counter.INSTRUCTION, ValueType.COVERED, BigDecimal.valueOf(5), BigDecimal.valueOf(52))
        val bound2_2 = Bound(2, Counter.BRANCH, ValueType.MISSED_RATE, BigDecimal.valueOf(0.0), null)
        rules.add(createRule(Target.ALL, bound2_1, bound2_2))

        val bound3_1 = Bound(1, Counter.LINE, ValueType.MISSED, BigDecimal.valueOf(10), BigDecimal.valueOf(70))
        val bound3_2 =
            Bound(2, Counter.INSTRUCTION, ValueType.COVERED_RATE, BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.9))
        rules.add(createRule(Target.ALL, bound3_1, bound3_2))

        runVerifier(rules, emptyList())
    }

    @Test
    @Throws(IOException::class, InterruptedException::class)
    fun test3() {
        val rules: MutableList<Rule> = ArrayList()
        val bound1_1 = Bound(1, Counter.LINE, ValueType.COVERED, BigDecimal.valueOf(10), BigDecimal.valueOf(15))
        rules.add(createRule(Target.PACKAGE, bound1_1))

        val boundViolation = BoundViolation(1)
        boundViolation.minViolations.add(Violation("", BigDecimal.ONE))
        boundViolation.minViolations.add(Violation("testData.inline", BigDecimal("3")))
        boundViolation.minViolations.add(Violation("testData.defaultArgs", BigDecimal.ZERO))
        boundViolation.minViolations.add(Violation("testData.outOfPackageStructure", BigDecimal.ZERO))
        boundViolation.minViolations.add(Violation("testData.simple", BigDecimal("5")))
        boundViolation.minViolations.add(Violation("testData.crossinline", BigDecimal.ZERO))
        boundViolation.minViolations.add(Violation("testData.noReport.branches", BigDecimal.ZERO))
        boundViolation.minViolations.add(Violation("testData.branches", BigDecimal("4")))

        val ruleViolation = RuleViolation(1, listOf(boundViolation))
        runVerifier(rules, listOf(ruleViolation))
    }

    @get:Throws(IOException::class)
    private val file: File
        get() = File.createTempFile("report", "ic")

    private var ruleId = 1

    @Throws(IOException::class)
    private fun createRule(target: Target, vararg bounds: Bound): Rule {
        return Rule(ruleId++, file, target, Arrays.asList(*bounds))
    }

    companion object {
        @Throws(IOException::class, InterruptedException::class)
        fun runVerifier(rules: List<Rule>, expected: List<RuleViolation>) {
            val requests: MutableList<Request> = ArrayList()
            val includes: MutableList<Pattern> = ArrayList()
            includes.add(Pattern.compile("testData\\.branches\\..*"))
            includes.add(Pattern.compile("testData\\.crossinline\\..*"))
            includes.add(Pattern.compile("testData\\.defaultArgs\\..*"))
            includes.add(Pattern.compile("testData\\.inline\\..*"))
            includes.add(Pattern.compile("testData\\.noReport\\..*"))
            includes.add(Pattern.compile("testData\\.simple\\..*"))
            includes.add(Pattern.compile("testData\\.outOfPackageStructure\\..*"))
            includes.add(Pattern.compile("[^.]*"))
            for (rule in rules) {
                val request = Request(
                    createFilters(includes),
                    rule.reportFile, null
                )
                requests.add(request)
            }
            clearLogFile(File("."))
            AggregatorTest.runAggregator(
                requests,
                "testData.branches.TestKt",
                "testData.inline.TestKt",
                "testData.simple.Main",
                "TestTopLevelKt"
            )
            checkLogFile(File("."))


            clearLogFile(File("."))
            val actual = VerificationApi.verify(rules)
            checkLogFile(File("."))
            check(expected, actual)
        }

        private fun check(expected: List<RuleViolation>, actual: List<RuleViolation>) {
            Assert.assertEquals(expected.size.toLong(), actual.size.toLong())

            for (i in expected.indices) {
                val expectedRule = expected[i]
                val actualRule = actual[i]
                Assert.assertEquals(expectedRule.id.toLong(), actualRule.id.toLong())

                Assert.assertEquals(expectedRule.violations.size.toLong(), actualRule.violations.size.toLong())
                for (j in expectedRule.violations.indices) {
                    val expectedBound = expectedRule.violations[j]
                    val actualBound = actualRule.violations[j]
                    Assert.assertEquals(expectedBound.id.toLong(), actualBound.id.toLong())

                    checkViolations(expectedBound.maxViolations, actualBound.maxViolations)
                    checkViolations(expectedBound.minViolations, actualBound.minViolations)
                }
            }
        }

        private fun checkViolations(expected: List<Violation>, actual: List<Violation>) {
            Assert.assertEquals(expected.size.toLong(), actual.size.toLong())

            for (i in expected.indices) {
                val expectedV = expected[i]
                val actualV = actual[i]

                Assert.assertEquals(expectedV.targetName, actualV.targetName)
                Assert.assertEquals(expectedV.targetValue, actualV.targetValue)
            }
        }
    }
}
