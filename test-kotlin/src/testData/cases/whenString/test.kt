/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package testData.cases.whenString

// ignore: Not implemented

fun foo(x: String) = when (x) { // coverage: FULL
    "a", "b" -> println(5) // coverage: FULL
    "c" -> println(6) // coverage: FULL
    else -> println(7) // coverage: FULL
}

// Here Aa and BB have the same hashCode
fun boo(x: String) = when (x) { // coverage: FULL
    "Aa", "BB" -> println(5) // coverage: FULL
    "c" -> println(6) // coverage: FULL
    "d" -> println(7) // coverage: FULL
    "e" -> println(8) // coverage: FULL
    "f" -> println(9) // coverage: FULL
    else -> println(10) // coverage: FULL
}

fun main() {
    foo("a") // coverage: FULL
    foo("b") // coverage: FULL
    foo("c") // coverage: FULL
    foo("d") // coverage: FULL

    boo("Aa") // coverage: FULL
    boo("BB") // coverage: FULL
    boo("c") // coverage: FULL
    boo("d") // coverage: FULL
    boo("e") // coverage: FULL
    boo("f") // coverage: FULL
    boo("g") // coverage: FULL
}
