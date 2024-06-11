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

package testData.cases.whenBoolean

import kotlin.random.Random

fun test(value: Boolean) {
    return when (value) {                            // coverage: FULL
        true -> Unit                                 // coverage: FULL // branches: 2/2
        false -> Unit                                // coverage: FULL
    }
}

fun testTrue(value: Boolean) {
    return when (value) {                            // coverage: FULL
        true -> Unit                                 // coverage: PARTIAL // branches: 1/2
        false -> Unit                                // coverage: FULL
    }
}

fun testFalse(value: Boolean) {
    return when (value) {                            // coverage: FULL
        true -> Unit                                 // coverage: PARTIAL // branches: 1/2
        false -> Unit                                // coverage: NONE
    }
}

fun testNestedWhen(value: Boolean) {
    return when (value) {                            // coverage: FULL
        true ->                                      // coverage: FULL // branches: 2/2
            if (Random.nextBoolean()) Unit else Unit // coverage: PARTIAL // branches: 1/2
        false -> Unit                                // coverage: FULL
    }
}

fun main() {
    test(true)                                       // coverage: FULL
    test(false)                                      // coverage: FULL
    testFalse(true)                                  // coverage: FULL
    testTrue(false)                                  // coverage: FULL
    testNestedWhen(true)                             // coverage: FULL
    testNestedWhen(false)                            // coverage: FULL
}
