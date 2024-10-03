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

package testData.custom.methodReference

// calculate unloaded: true

class Foo {         // coverage: FULL
    var x: Int = 42 // coverage: FULL
    val y = "Hey"   // coverage: FULL

    fun bar() {
        print("hello") // coverage: NONE
    } // coverage: NONE
}

fun biz() {
    val foo = Foo()   // coverage: FULL
    println(foo::bar) // coverage: FULL
    println(foo::x)   // coverage: FULL
    println(foo::y)   // coverage: FULL
} // coverage: FULL

fun main() = biz() // coverage: FULL
