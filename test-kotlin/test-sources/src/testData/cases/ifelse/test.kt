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

package testData.cases.ifelse

// instructions & branches
// offline instrumentation

fun test(x: Boolean, y: Boolean, z: Boolean) {
    if (x) {                            // coverage: PARTIAL // branches: 1/2 // stats: 2/2
        println("X is true")            // coverage: FULL    // stats: 5/5
    } else {
        println("X is false")           // coverage: NONE    // stats: 0/4
    }
    if (y) {                            // coverage: PARTIAL // branches: 1/2 // stats: 2/2
        println("Y is true")            // coverage: NONE    // stats: 0/4
    }
    if (z) {                            // coverage: PARTIAL // branches: 1/2 // stats: 2/2
        println("Y is true")            // coverage: FULL    // stats: 4/4
        return // coverage: FULL
    }
} // coverage: NONE

fun test2(x: Boolean, y: Boolean) {
    if (x) {                            // coverage: FULL // branches: 2/2 // stats: 2/2
        println("X is true")            // coverage: FULL    // stats: 5/5
    } else {
        println("X is false")           // coverage: FULL    // stats: 4/4
    }
    if (y) {                            // coverage: PARTIAL // branches: 1/2 // stats: 2/2
        println("Y is true")            // coverage: FULL    // stats: 4/4
    }
} // coverage: FULL


fun test3(b: Boolean) {
    if (!b) {                           // coverage: PARTIAL // branches: 1/2 // stats: 2/2
        while (b) {                     // coverage: PARTIAL // branches: 1/2 // stats: 2/2
            println("")                 // coverage: NONE    // stats: 0/5
        }
    }
} // coverage: FULL

fun foo(x: Boolean): Int? = if (x) 3 else null  // coverage: FULL // branches: 2/2 // stats: 6/6
fun foo1(x: Boolean): Int? = if (x) 3 else null // coverage: PARTIAL // branches: 1/2 // stats: 5/6
fun foo2(x: Boolean): Int? = if (x) 3 else null // coverage: PARTIAL // branches: 1/2 // stats: 3/6

fun test4() {
    foo(true) ?: foo(false)  // coverage: PARTIAL // branches: 1/2 // stats: 5/8
    foo(false) ?: foo(true)  // coverage: PARTIAL // branches: 1/2 // stats: 7/8
    foo(false) ?: foo(false) // coverage: PARTIAL // branches: 1/2 // stats: 7/8
    foo(true) ?: foo(true)   // coverage: PARTIAL // branches: 1/2 // stats: 5/8
    foo1(true)               // coverage: FULL    // stats: 3/3
    foo2(false)              // coverage: FULL    // stats: 3/3
} // coverage: FULL

fun main() {
    test(x = true, y = false, z = true) // coverage: FULL // stats: 4/4
    test2(x = true, y = true)           // coverage: FULL // stats: 3/3
    test2(x = false, y = true)          // coverage: FULL // stats: 3/3
    test3(false)                        // coverage: FULL // stats: 2/2
    test4()                             // coverage: FULL // stats: 1/1
} // coverage: FULL
