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

package testData.ignoreAnnotation.inlineUnloaded

import testData.ignoreAnnotation.IgnoreCoverage

// classes: UnusedClass AbsolutelyUnusedClass
// calculate unloaded: true
// patterns: testData.ignoreAnnotation.inlineUnloaded.* -excludeAnnotations testData.ignoreAnnotation.IgnoreCoverage

object UnusedClass {      // coverage: NONE
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

object AbsolutelyUnusedClass {      // coverage: NONE
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

fun main() {
    UnusedClass.foo()
}
