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

package kotlinTestData.coroutines.lambda

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun test() {
    GlobalScope.launch {           // coverage: FULL
        delay(10)                  // coverage: FULL
        println("World!")          // coverage: FULL
        delay(10)                  // coverage: FULL
        println("From coroutines") // coverage: FULL
    }
    println("Hello,")              // coverage: FULL
    Thread.sleep(100L)             // coverage: FULL
}

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        test()
    }
}
