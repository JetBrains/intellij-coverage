/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

package testData.ignoreAnnotation.anonymous

import testData.ignoreAnnotation.IgnoreCoverage

// patterns: testData.ignoreAnnotation.anonymous.* -excludeAnnotations testData.ignoreAnnotation.IgnoreCoverage
// classes: ALL
// calculate unloaded: true

@IgnoreCoverage
fun boo() {
    functionWithLambda {
        functionWithLambda {
            it + 42
        }
        it + 42
    }
    println("boo")
}

@IgnoreCoverage
fun booWithInner() {
    fun localFun() {
        fun localFun2() {
            println()
        }
        println()
    }
    println("boo")
}

// TODO !!! here ignoring is false positive due to the same outer method name
fun booWithInner(x: Int) {
    fun localFun() {
        fun localFun2() {
            println()
        }
        println()
    }
    println("boo") // coverage: FULL
}


fun functionWithLambda(lambda: (Int) -> Int) {
    print(lambda(5)) // coverage: FULL
}

fun main() {
    boo() // coverage: FULL
    booWithInner() // coverage: FULL
    booWithInner(2) // coverage: FULL
}
