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

package kotlinTestData.testTracking.sequentialTests

import kotlinTestData.testTracking.runTestTracking
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import kotlin.system.exitProcess

private fun doWork(limit: Int = 5): Int {
    var result = 1
    for (i in 1..limit) {
        result += doWork(limit - 1)
    }
    return result
}

private class Class0 {
    fun foo() {
        doWork()                           // tests: Test1 Test2 Test3...
    }
}

private class Class1 {
    fun foo() {
        doWork()                           // tests: Test1 Test2 Test3...
    }
}

private class Class2 {
    fun foo() {
        doWork()                           // tests: Test1 Test2 Test3...
    }
}

private class Class3 {
    fun foo() {
        doWork()                           // tests: Test1 Test2 Test3...
    }
}

private class Class4 {
    fun foo() {
        doWork()                           // tests: Test1 Test2 Test3...
    }
}

private const val THREADS = 10
internal const val TESTS = 1000
private const val CLASSES = 5

private val testMethods: List<() -> Any> = List(CLASSES) {
    val clazz = Class.forName("kotlinTestData.testTracking.sequentialTests.Class$it")
    val instance = clazz.getConstructor().newInstance()
    val method = clazz.getMethod("foo")
    return@List { method.invoke(instance) }
}

private val tasks = List(THREADS) { iClass ->
    Runnable {
        runCatching {
            testMethods[iClass % CLASSES]()
        }.onFailure { it.printStackTrace(System.err); exitProcess(1) }
        barrier.await()
    }
}

private val barrier = CyclicBarrier(THREADS + 1)

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        testMethods
        val pool = Executors.newFixedThreadPool(THREADS)
        repeat(TESTS) { iTest ->
            barrier.reset()
            runTestTracking("Test$iTest") {
                tasks.shuffled().forEach {
                    pool.execute(it)
                }
                barrier.await()
            }
        }
        check(pool.shutdownNow().isEmpty())
    }
}
