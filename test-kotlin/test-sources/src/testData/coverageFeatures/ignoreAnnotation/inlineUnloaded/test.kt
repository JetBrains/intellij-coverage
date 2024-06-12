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

package testData.coverageFeatures.ignoreAnnotation.inlineUnloaded

import testData.coverageFeatures.ignoreAnnotation.IgnoreCoverage

// calculate unloaded: true
// patterns: testData.coverageFeatures.ignoreAnnotation.inlineUnloaded.* -excludeAnnotations testData.coverageFeatures.ignoreAnnotation.IgnoreCoverage

// class: UnusedClass
object UnusedClass {
    @IgnoreCoverage
    @JvmStatic
    inline fun foo() {
        println("foo")
    }

    @JvmStatic
    fun boo() {
        println("boo")    // coverage: NONE
    }
}

// class: AbsolutelyUnusedClass
object AbsolutelyUnusedClass {
    @IgnoreCoverage
    @JvmStatic
    inline fun foo() {
        println("foo")
    }

    @JvmStatic
    fun boo() {
        foo()             // coverage: NONE
        println("boo")    // coverage: NONE
    }
}

// class: TestKt
fun main() {
    UnusedClass.foo() // coverage: FULL
}
