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

package testData.inline.reified

inline fun <reified T> createArray(size: Int): Any = when (T::class) { // coverage: FULL
    Int::class ->                                                      // coverage: FULL // branches: 2/2
        IntArray(size)                                                 // coverage: FULL
    Long::class ->                                                     // coverage: PARTIAL // branches: 1/2
        LongArray(size)                                                // coverage: NONE
    else ->
        Array<T?>(size) { null }                                       // coverage: FULL // branches: 2/2
}

inline fun <reified T> get(key: String): String = when (T::class) { // coverage: FULL
    String::class -> "String"                                       // coverage: PARTIAL // branches: 1/2
    else -> "no"                                                    // coverage: NONE
}

fun main() {
    val a = createArray<Int>(10)                                       // coverage: FULL
    val b = createArray<Any>(30)                                       // coverage: FULL
    assert(get<String>("String") == "String")                       // coverage: PARTIAL // branches: 1/4
}
