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

package kotlinTestData.threadSafe.data

import java.util.concurrent.CyclicBarrier

class SimpleClass {
    private var x = 0
    fun foo() {
        x++
    }
}

private const val THREADS = 2
private const val ITERATIONS = 1000
const val THREAD_SAFE_DATA_EXPECTED_HITS = THREADS * ITERATIONS

private val barrier = CyclicBarrier(THREADS + 1)
private val instance = SimpleClass()

private val tasks = List(THREADS) {
    Runnable {
        repeat(ITERATIONS) {
            instance.foo()
        }
        barrier.await()
    }
}

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        repeat(THREADS) { i ->
            Thread(tasks[i]).start()
        }
        barrier.await()
    }
}
