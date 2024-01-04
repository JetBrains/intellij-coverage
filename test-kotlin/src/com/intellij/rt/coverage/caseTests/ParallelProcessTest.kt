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

package com.intellij.rt.coverage.caseTests

import com.intellij.rt.coverage.Coverage
import com.intellij.rt.coverage.CoverageTest
import com.intellij.rt.coverage.TestConfiguration
import com.intellij.rt.coverage.assertEqualsLines
import com.intellij.rt.coverage.data.ProjectData
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

internal class ParallelProcessTest : CoverageTest() {
    @Test
    fun testTwoProcessesAccessOneFileInParallel() {
        val stop = AtomicBoolean(false)
        val threads = List(5) { Thread { runProcess(stop) } }
        var exception: Throwable? = null
        val exceptionHandler = Thread.UncaughtExceptionHandler { _, e ->
            exception = e
            stop.set(true)
        }
        threads
                .onEach { it.uncaughtExceptionHandler = exceptionHandler }
                .onEach { it.start() }
                .onEach { it.join() }
        if (exception != null) {
            throw exception!!
        }
    }

    private fun runProcess(stop: AtomicBoolean) {
        repeat(10) {
            if (stop.get()) return
            test("simple.ifelse")
        }
    }

    override val coverage get() = Coverage.BRANCH_FIELD
    override fun verifyResults(projectData: ProjectData, configuration: TestConfiguration) {
        assertEqualsLines(projectData, configuration, coverage)
    }
}
