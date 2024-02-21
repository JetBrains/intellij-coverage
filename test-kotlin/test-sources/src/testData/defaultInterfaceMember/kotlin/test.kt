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

package testData.defaultInterfaceMember.kotlin

// classes: Foo$DefaultImpls Bar

interface Foo {
    fun foo1() {
        println() // coverage: NONE
    }

    fun foo2() {
        println() // coverage: FULL
    }

    fun foo3() {
        println() // coverage: NONE
    }
}
// this line is invisible only in Kotlin <= 1.4 for coverage as default member is covered in Foo
class Bar         // coverage: FULL
() : Foo {
    override fun foo1() {
        println() // coverage: NONE
    }
}

fun main() {
    Bar().foo2()
}
