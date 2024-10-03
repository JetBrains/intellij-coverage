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

package testData.kotlinFeatures.valueClass

@JvmInline
value class MyValueClass(val data: String) {  // coverage: FULL
    val boo get() = 42 // coverage: NONE
    fun foo() {
        println(data) // coverage: FULL
    } // coverage: FULL
}

@JvmInline
value class MyValueClass2( // coverage: FULL
    val data: String
) {
    val boo get() = 42 // coverage: NONE
    fun foo() {
        println(data) // coverage: FULL
    } // coverage: FULL
}

@JvmInline
value class MyValueClass3( // coverage: NONE
    val data: String
) {
    val boo get() = 42 // coverage: NONE
    fun foo() {
        println(data) // coverage: NONE
    } // coverage: NONE
}

@JvmInline
value class MyValueClass4(val data: String) { // coverage: NONE
    val boo get() = 42 // coverage: NONE
    fun foo() {
        println(data) // coverage: NONE
    } // coverage: NONE
}

fun main() {
    val v = MyValueClass("ABC") // coverage: FULL
    if (v.data != "ABC") // coverage: PARTIAL // branches: 1/2
        error("not abc") // coverage: NONE
    v.foo() // coverage: FULL
    val v2 = MyValueClass2("DEF") // coverage: FULL
    v2.foo() // coverage: FULL
    Class.forName(MyValueClass3::class.qualifiedName) // coverage: FULL
    Class.forName(MyValueClass4::class.qualifiedName) // coverage: FULL
} // coverage: FULL
