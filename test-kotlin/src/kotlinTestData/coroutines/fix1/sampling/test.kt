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

package kotlinTestData.coroutines.fix1.sampling

import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

suspend fun foo1(useStateMachineCodePath: Boolean) {
    println(1)                            // coverage: FULL
    if (useStateMachineCodePath) {        // coverage: FULL
        yield()                           // coverage: NONE
    }
    ctxOuter(useStateMachineCodePath)     // coverage: FULL
    println(2)                            // coverage: FULL
}

suspend inline fun ctxOuter(useStateMachineCodePath: Boolean) {
    ctx(useStateMachineCodePath)          // coverage: FULL
    println("Done")                       // coverage: FULL
}                                         // coverage: FULL

suspend inline fun ctx(useStateMachineCodePath: Boolean) {
    if (useStateMachineCodePath) {                       // coverage: FULL
        yield()                                          // coverage: NONE
    }
    println(coroutineContext[Job])                       // coverage: FULL
    coroutineScope {                                     // coverage: FULL
        println(kotlin.coroutines.coroutineContext[Job]) // coverage: FULL
    }
    println(coroutineContext[Job])                       // coverage: FULL
}                                                        // coverage: FULL

suspend fun foo2(useStateMachineCodePath: Boolean) {
    println(1)                            // coverage: FULL
    if (useStateMachineCodePath) {        // coverage: FULL
        yield()                           // coverage: NONE
    }
    ctxOuter2(useStateMachineCodePath)    // coverage: FULL
    println(2)                            // coverage: FULL
}

suspend fun ctxOuter2(useStateMachineCodePath: Boolean) {
    ctx2(useStateMachineCodePath)         // coverage: FULL
    println("Done")                       // coverage: FULL
}

suspend fun ctx2(useStateMachineCodePath: Boolean) {
    if (useStateMachineCodePath) {                       // coverage: FULL
        yield()                                          // coverage: NONE
    }
    println(coroutineContext[Job])                       // coverage: FULL
    coroutineScope {                                     // coverage: FULL
        println(kotlin.coroutines.coroutineContext[Job]) // coverage: FULL
    }
    println(coroutineContext[Job])                       // coverage: FULL
}

object Test {                                      // coverage: FULL
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {  // coverage: FULL
        foo1(false)                                // coverage: FULL
        foo2(false)                                // coverage: FULL
    }                                              // coverage: FULL
}

