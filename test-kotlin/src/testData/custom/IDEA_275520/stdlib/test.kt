/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package testData.custom.IDEA_275520.stdlib

// patterns: testData.custom.IDEA_275520.*
// calculate unloaded: true

fun main() {
    listOf(1).forEach { println(it) }
    Result.success(42).getOrNull()
    "hello".indexOfFirst { it == 'a' }
    foo()
    listOf("a", "b", "c").reindent()
}

inline fun List<String>.reindent(): String {
    return mapIndexedNotNull { index, value ->
        if (index == 0) null
        else value
    }.toString()
}

private fun foo() {
    require("".isBlank()) { "" }
    listOf("").mapNotNull2 { line ->
        line.indexOfFirst { it == 'x' }
    }
}

inline fun List<String>.mapNotNull2(f: (String) -> Unit) {
    mapNotNull { f(it) }
}
