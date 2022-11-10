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

package testData.defaultArgs.simple

import kotlin.random.Random

// classes: TestKt X

class X {                                       // coverage: FULL
    fun functionWithDefaultArguments(
        x: Int = 3,                             // coverage: FULL , generated if is ignored
        y: Int = 42                             // coverage: FULL
    ): Int {
        return x + y                            // coverage: FULL
    }
}


private fun functionWithDefaultArguments(
    x: Int = 3,                                 // coverage: FULL , generated if is ignored
    y: Int = 42                                 // coverage: FULL
): Int {
    return x + y                                // coverage: FULL
}


private fun functionWithDefaultArgumentsWithIf(
    x: Int = 2,                                 // coverage: FULL , generated if is ignored
    y: Int = 42,                                // coverage: FULL
    z: Int = if (Random.nextBoolean()) 3 else 4 // coverage: PARTIAL
): Int {
    return x + y                                // coverage: FULL
}

fun main() {
    functionWithDefaultArguments()              // coverage: FULL
    X().functionWithDefaultArguments()          // coverage: FULL
    functionWithDefaultArgumentsWithIf()        // coverage: FULL
}
