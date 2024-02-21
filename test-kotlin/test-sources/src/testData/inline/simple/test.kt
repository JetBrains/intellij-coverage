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

package testData.inline.simple

import kotlin.random.Random

private inline fun a(x: Int) {
    println(x) // coverage: FULL
}

private inline fun a() {
    println()  // coverage: FULL
}

inline fun funWithCondition() {
    if (Random.nextBoolean()) { // coverage: NONE
        println("Success")      // coverage: NONE
    } else {
        println("Fail")         // coverage: NONE
    }
}

fun main() {
    a(4)       // coverage: FULL
    a()        // coverage: FULL
}
