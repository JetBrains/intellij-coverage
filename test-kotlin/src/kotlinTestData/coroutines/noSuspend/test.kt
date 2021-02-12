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

package kotlinTestData.coroutines.noSuspend

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

suspend fun test(): Int {
    return f()       // coverage: FULL
}

suspend fun noSuspend() {
    println("Hello") // coverage: FULL
}

suspend fun f(): Int {
    delay(1)         // coverage: FULL
    return 42        // coverage: FULL
}

object Test {
    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
        test()
        noSuspend()
    }
}
