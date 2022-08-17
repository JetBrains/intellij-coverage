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

package testData.inline.coroutines

import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

// classes: ALL

suspend fun foo1(flag: Boolean) {
    if (flag) {                                          // coverage: FULL
        foo1(false)                                      // coverage: FULL
    } else {
        println(foo {                                    // coverage: FULL
            ctxOuter()                                   // coverage: FULL
        })                                               // coverage: FULL
    }
}

suspend inline fun ctxOuter() {
    ctx()                                                // coverage: FULL
    println("Done")                                      // coverage: FULL
}

suspend inline fun ctx() {
    println(coroutineContext[Job])                       // coverage: FULL
    coroutineScope {                                     // coverage: FULL
        println(kotlin.coroutines.coroutineContext[Job]) // coverage: FULL
    }
    println(coroutineContext[Job])                       // coverage: FULL
}

suspend inline fun foo(crossinline block: suspend () -> Unit) {
    yield()                                              // coverage: FULL
    block()                                              // coverage: FULL
    println("Aha!")                                      // coverage: FULL
}

fun test() = runBlocking {                               // coverage: FULL
    foo1(true)                                           // coverage: FULL
}

fun main() {
    test()                                               // coverage: FULL
}
