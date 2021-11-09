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

package testData.simple.ifelse

fun test(x: Boolean, y: Boolean, z: Boolean) {
    if (x) {                            // coverage: PARTIAL
        println("X is true")            // coverage: FULL
    } else {
        println("X is false")           // coverage: NONE
    }
    if (y) {                            // coverage: PARTIAL
        println("Y is true")            // coverage: NONE
    }
    if (z) {                            // coverage: PARTIAL
        println("Y is true")            // coverage: FULL
        return
    }
}

fun test2(x: Boolean, y: Boolean) {
    if (x) {                            // coverage: FULL
        println("X is true")            // coverage: FULL
    } else {
        println("X is false")           // coverage: FULL
    }
    if (y) {                            // coverage: PARTIAL
        println("Y is true")            // coverage: FULL
    }
}


fun test3(b: Boolean) {
    if (!b) {                           // coverage: PARTIAL
        while (b) {                     // coverage: PARTIAL
            println("")                 // coverage: NONE
        }
    }
}

fun foo(x: Boolean): Int? = if (x) 3 else null // coverage: FULL

fun test4() {
    foo(true) ?: foo(false)  // coverage: PARTIAL
    foo(false) ?: foo(true)  // coverage: PARTIAL
    foo(false) ?: foo(false) // coverage: PARTIAL
    foo(true) ?: foo(true)   // coverage: PARTIAL
}

fun main(args: Array<String>) {
    test(x = true, y = false, z = true) // coverage: FULL
    test2(x = true, y = true)           // coverage: FULL
    test2(x = false, y = true)          // coverage: FULL
    test3(false)                        // coverage: FULL
    test4()                             // coverage: FULL
}
