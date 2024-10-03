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

package testData.coroutines.async

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

fun test() = runBlocking {                                    // coverage: FULL
    val time = measureTimeMillis {                            // coverage: FULL
        val one = async {                                     // coverage: FULL
            doSomethingUsefulOne()                            // coverage: FULL
        }
        val two = async {                                     // coverage: FULL
            doSomethingUsefulTwo()                            // coverage: FULL
        }
        println("The answer is ${one.await() + two.await()}") // coverage: FULL
    }                                                         // coverage: FULL
    println("Completed in $time ms")                          // coverage: FULL
} // coverage: FULL

suspend fun doSomethingUsefulOne(): Int {
    delay(100)                                                // coverage: FULL
    return 13                                                 // coverage: FULL
}

suspend fun doSomethingUsefulTwo(): Int {
    delay(100)                                                // coverage: FULL
    return 29                                                 // coverage: FULL
}


fun main(): Unit = runBlocking {                              // coverage: FULL
    test()                                                    // coverage: FULL
} // coverage: FULL
