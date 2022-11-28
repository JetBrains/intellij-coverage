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

// instructions & branches

fun foo(x: String) = when (x) { // coverage: FULL // stats: 17/17 3/3
    "a", "b" -> println(5) // coverage: FULL // stats: 6/6
    "c" -> println(6) // coverage: FULL // stats: 6/6
    else -> println(7) // coverage: FULL // stats: 5/5
}

// extra if is generated here to check null
fun foo2(x: String?) = when (x) { // coverage: FULL // stats: 16/16 3/3
    "a", "b" -> println(5) // coverage: FULL // stats: 6/6
    "c" -> println(6) // coverage: FULL // stats: 6/6
    else -> println(7) // coverage: FULL // stats: 5/5
}

// no GOTO is generated
fun foo3(x: String) = when (x) { // coverage: FULL // stats: 17/17 3/3
    "c" -> println(6) // coverage: FULL // stats: 6/6
    "a", "b" -> println(5) // coverage: FULL // stats: 6/6
    else -> println(7) // coverage: FULL // stats: 5/5
}

// Here Aa and BB have the same hashCode
fun boo(x: String) = when (x) { // coverage: FULL // stats: 29/29 7/7
    "Aa", "BB" -> println(5) // coverage: FULL // stats: 6/6
    "c" -> println(6) // coverage: FULL // stats: 6/6
    "d" -> println(7) // coverage: FULL // stats: 6/6
    "e" -> println(8) // coverage: FULL // stats: 6/6
    "f" -> println(9) // coverage: FULL  // stats: 6/6
    else -> println(10) // coverage: FULL // stats: 5/5
}

// Here Aa and BB have the same hashCode
fun boo2(x: String) = when (x) { // coverage: PARTIAL // stats: 25/29 6/7
    "Aa", "BB" -> println(5) // coverage: FULL // stats: 6/6
    "c" -> println(6) // coverage: FULL // stats: 6/6
    "d" -> println(7) // coverage: FULL // stats: 6/6
    "e" -> println(8) // coverage: FULL // stats: 6/6
    "f" -> println(9) // coverage: FULL // stats: 6/6
    else -> println(10) // coverage: FULL // stats: 5/5
}

// No switch generated
fun voo(x: String) = when (x) { // coverage: FULL // stats: 2/2
    "Aa", "BB" -> println(5) // coverage: FULL // stats: 16/16 4/4
    else -> println(10) // coverage: FULL // stats: 5/5
}

fun main() {
    foo("a") // coverage: FULL // stats: 2/2
    foo("b") // coverage: FULL // stats: 2/2
    foo("c") // coverage: FULL // stats: 2/2
    foo("d") // coverage: FULL // stats: 2/2

    foo2("a") // coverage: FULL // stats: 2/2
    foo2("b") // coverage: FULL // stats: 2/2
    foo2("c") // coverage: FULL // stats: 2/2
    foo2("d") // coverage: FULL // stats: 2/2

    foo3("a") // coverage: FULL // stats: 2/2
    foo3("b") // coverage: FULL // stats: 2/2
    foo3("c") // coverage: FULL // stats: 2/2
    foo3("d") // coverage: FULL // stats: 2/2

    boo("Aa") // coverage: FULL // stats: 2/2
    boo("BB") // coverage: FULL // stats: 2/2
    boo("c") // coverage: FULL // stats: 2/2
    boo("d") // coverage: FULL // stats: 2/2
    boo("e") // coverage: FULL // stats: 2/2
    boo("f") // coverage: FULL // stats: 2/2
    boo("g") // coverage: FULL // stats: 2/2

    boo2("Aa") // coverage: FULL // stats: 2/2
    boo2("c") // coverage: FULL // stats: 2/2
    boo2("d") // coverage: FULL // stats: 2/2
    boo2("e") // coverage: FULL // stats: 2/2
    boo2("f") // coverage: FULL // stats: 2/2
    boo2("g") // coverage: FULL // stats: 2/2

    voo("Aa") // coverage: FULL // stats: 2/2
    voo("BB") // coverage: FULL // stats: 2/2
    voo("g") // coverage: FULL // stats: 2/2
}
