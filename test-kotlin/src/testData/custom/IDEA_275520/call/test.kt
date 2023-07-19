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

package testData.custom.IDEA_275520.call

// patterns: testData.custom.IDEA_275520.*
// calculate unloaded: true

object SomeObject {
    @JvmStatic
    inline fun foo(f: (Int) -> Int) {
        println(f(3) + 10)
    }

    @JvmStatic
    inline fun boo(f: (Int) -> Int) {
        println(f(3) + 10)
    }
}

inline fun Int.sum() = this

fun main() {
    SomeObject.foo { 42 }
    SomeObject.foo { SomeObject.boo { 42 }; 43}
    5.sum()
}
