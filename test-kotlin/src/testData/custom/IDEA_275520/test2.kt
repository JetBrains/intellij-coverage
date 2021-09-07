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

package testData.custom.IDEA_275520

inline fun simpleInline(x: Int) {
    println("Test")
}

inline fun nestedInlines(x: Int) {
    println("Hello")
    simpleInline(x)
    println()
    simpleInline(x)
    simpleInline(x)
}

inline fun oneLineInline() = 42

inline fun withLambda(f: () -> Int): Int {
    return f() + 10
}

inline fun testWithLambda() {
    withLambda { 4 }
}
