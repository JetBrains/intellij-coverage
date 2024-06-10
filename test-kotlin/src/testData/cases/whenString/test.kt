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

// with branches

fun foo(x: String) = when (x) { // coverage: FULL // branches: 3/3
    "a", "b" -> println(5) // coverage: FULL 
    "c" -> println(6) // coverage: FULL 
    else -> println(7) // coverage: FULL 
}

// extra if is generated here to check null
fun foo2(x: String?) = when (x) { // coverage: FULL // branches: 3/3
    "a", "b" -> println(5) // coverage: FULL 
    "c" -> println(6) // coverage: FULL 
    else -> println(7) // coverage: FULL 
}

// no GOTO is generated
fun foo3(x: String) = when (x) { // coverage: FULL // branches: 3/3
    "c" -> println(6) // coverage: FULL 
    "a", "b" -> println(5) // coverage: FULL 
    else -> println(7) // coverage: FULL 
}

// Here Aa and BB have the same hashCode
fun boo(x: String) = when (x) { // coverage: FULL // branches: 7/7
    "Aa", "BB" -> println(5) // coverage: FULL 
    "c" -> println(6) // coverage: FULL 
    "d" -> println(7) // coverage: FULL 
    "e" -> println(8) // coverage: FULL 
    "f" -> println(9) // coverage: FULL  
    else -> println(10) // coverage: FULL 
}

// Here Aa and BB have the same hashCode
fun boo2(x: String) = when (x) { // coverage: PARTIAL // branches: 6/7
    "Aa", "BB" -> println(5) // coverage: FULL 
    "c" -> println(6) // coverage: FULL 
    "d" -> println(7) // coverage: FULL 
    "e" -> println(8) // coverage: FULL 
    "f" -> println(9) // coverage: FULL 
    else -> println(10) // coverage: FULL 
}

// No switch generated
fun voo(x: String) = when (x) { // coverage: FULL 
    "Aa", "BB" -> println(5) // coverage: FULL // branches: 4/4
    else -> println(10) // coverage: FULL 
}

fun main() {
    foo("a") // coverage: FULL 
    foo("b") // coverage: FULL 
    foo("c") // coverage: FULL 
    foo("d") // coverage: FULL 

    foo2("a") // coverage: FULL 
    foo2("b") // coverage: FULL 
    foo2("c") // coverage: FULL 
    foo2("d") // coverage: FULL 

    foo3("a") // coverage: FULL 
    foo3("b") // coverage: FULL 
    foo3("c") // coverage: FULL 
    foo3("d") // coverage: FULL 

    boo("Aa") // coverage: FULL 
    boo("BB") // coverage: FULL 
    boo("c") // coverage: FULL 
    boo("d") // coverage: FULL 
    boo("e") // coverage: FULL 
    boo("f") // coverage: FULL 
    boo("g") // coverage: FULL 

    boo2("Aa") // coverage: FULL 
    boo2("c") // coverage: FULL 
    boo2("d") // coverage: FULL 
    boo2("e") // coverage: FULL 
    boo2("f") // coverage: FULL 
    boo2("g") // coverage: FULL 

    voo("Aa") // coverage: FULL 
    voo("BB") // coverage: FULL 
    voo("g") // coverage: FULL 
}
