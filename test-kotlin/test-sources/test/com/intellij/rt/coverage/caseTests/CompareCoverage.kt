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

package com.intellij.rt.coverage.caseTests

import com.intellij.rt.coverage.data.LineData
import com.intellij.rt.coverage.util.ProjectDataLoader
import com.intellij.rt.coverage.util.diff.CoverageDiff
import com.intellij.rt.coverage.util.diff.ReplaceDiffElement
import java.io.File

fun main() {
    val failedCoverage = ProjectDataLoader.load(File("/Users/maksim.zuev/IdeaProjects/kotlinx.coroutines/failed_coverage.ic"))
    val passedCoverage = ProjectDataLoader.load(File("/Users/maksim.zuev/IdeaProjects/kotlinx.coroutines/coverage.ic"))

    val diff = CoverageDiff.coverageDiff(passedCoverage, failedCoverage)
    val executedInFailed = diff.linesDiff.filterIsInstance<ReplaceDiffElement<LineData>>()
        .filter { (it.before.hits == 0) && (it.after.hits > 0) }
        .groupBy { it.className }
        .mapValues { it.value.map { it.after } }

    val notExecutedInFailed = diff.linesDiff.filterIsInstance<ReplaceDiffElement<LineData>>()
        .filter { (it.before.hits > 0) && (it.after.hits == 0) }
        .groupBy { it.className }
        .mapValues { it.value.map { it.before } }

    println("Executed in failed scenario:")
    println(executedInFailed.mapValues { it.value.map { it.lineNumber } }.entries.joinToString("\n"))
    println()

    println("Not executed in failed scenario:")
    println(notExecutedInFailed.mapValues { it.value.map { it.lineNumber } }.entries.joinToString("\n"))
}

///Users/maksim.zuev/IdeaProjects/kotlinx.coroutines/failed_coverage.ic
//false
//true
//false
//false
//kotlinx.coroutines.*

//    jvmArgs = mutableListOf(
//        "-javaagent:/Users/maksim.zuev/IdeaProjects/intellij-coverage/dist/intellij-coverage-agent.jar=/Users/maksim.zuev/IdeaProjects/kotlinx.coroutines/coverage.config",
//        "-Didea.coverage.calculate.hits=true",
//        "-Didea.coverage.log.level=info"
//    )

private fun callResetCoverage() {
    val clazz = try {
        Class.forName("com.intellij.rt.coverage.data.ProjectData")
    } catch (e: ClassNotFoundException) {
        return
    }
    clazz.getMethod("resetCoverageData").invoke(null)
}
