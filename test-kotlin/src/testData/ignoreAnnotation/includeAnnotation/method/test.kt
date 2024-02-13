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

package testData.ignoreAnnotation.includeAnnotation.method

import testData.ignoreAnnotation.IncludeCoverage

// calculate unloaded: true
// patterns: testData.ignoreAnnotation.includeAnnotation.method.* -includeAnnotations testData.ignoreAnnotation.IncludeCoverage

@IncludeCoverage
fun foo() {
    println("foo") // coverage: FULL
}

@IncludeCoverage
fun inlineFoo() {
    println("foo") // coverage: FULL
}

@IncludeCoverage
fun booWithDefault(x: Int = 2) { // coverage: NONE
    println("boo") // coverage: NONE
}

fun boo() {
    println("boo")
}

inline fun inlineBoo() {
    println("boo")
}

object AbsolutelyUnusedClass {
    @JvmStatic
    inline fun foo() {
        println("foo")
    }

    @IncludeCoverage
    @JvmStatic
    fun boo() {
        foo()             // coverage: NONE
        println("boo")    // coverage: NONE
    }
}

@IncludeCoverage
fun fooWithInternal() {
    fun local() {
        functionWithLambda { // coverage: NONE
            it + 2 // coverage: NONE
        }
    }
    local() // coverage: NONE
}

fun functionWithLambda(lambda: (Int) -> Int) {
    print(lambda(5))
}


fun main() {
    foo()
    boo()
    inlineFoo()
    inlineBoo()
}
