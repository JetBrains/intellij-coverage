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

package testData.nullability

// classes: TestKt

class A(val x: Int)

fun test(a: A?) {
    println(a?.x)             // coverage: PARTIAL
    println(a ?: "a is null") // coverage: PARTIAL
    println(a!!.x)            // coverage: FULL
}

fun test2(nullableField: String?) {
    // See https://github.com/Kotlin/kotlinx-kover/issues/368
    // `let { "Hello : $it" }` is always not-null here, but it is checked for nullability still
    val name = (nullableField?.let { "Hello : $it" })    // coverage: PARTIAL
        ?: "Nobody?"          // coverage: FULL
    println(name)             // coverage: FULL
}

fun test3(nullableField: String?) {
    val fooBar = nullableField?.let { "Hello : $it" } // coverage: FULL
    val name = fooBar ?: "Nobody?"                    // coverage: FULL
    println(name)             // coverage: FULL
}

fun main() {
    test(A(42))               // coverage: FULL
    test2(null)               // coverage: FULL
    test2("X")                // coverage: FULL
    test3(null)               // coverage: FULL
    test3("X")                // coverage: FULL
}
