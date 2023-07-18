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

package testData.ignoreAnnotation.fullClass

import kotlinx.serialization.Serializable
import testData.ignoreAnnotation.IgnoreCoverage


// patterns: testData.ignoreAnnotation.fullClass.* -excludeAnnotations testData.ignoreAnnotation.IgnoreCoverage kotlinx.serialization.Serializable
// classes: ALL
// calculate unloaded: true

// class: Foo
class Foo { // coverage: FULL
    fun foo() {
        println("foo") // coverage: FULL
    }
}

@IgnoreCoverage
class Boo {
    fun boo() {
        println("boo")
        functionWithLambda { 42 }
    }

    companion object {
        fun staticBoo() {
            println()
        }
    }

    // class: Boo$Foo
    class Foo { // coverage: FULL
        fun foo() {
            println() // coverage: FULL
        }
    }
}

@Serializable
data class DataClass(val i: Int) {
    companion object {
        fun foo() {
            println()
        }
    }
}

fun functionWithLambda(lambda: (Int) -> Int) {
    print(lambda(5)) // coverage: FULL
}

// class: TestKt
fun main() {
    Foo().foo() // coverage: FULL
    Boo().boo() // coverage: FULL
    Boo.staticBoo() // coverage: FULL
    Boo.Foo().foo() // coverage: FULL
}
