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

package testData.utilClass.kotlin

// calculate unloaded: true

object Util {        // should be ignored
    val x = foo2()                                // coverage: FULL
    var y = 4                                     // coverage: FULL
    fun foo1() {
        print(x)                                  // coverage: FULL
    }

    private fun foo2(): Int {
        y = 3                                     // coverage: FULL
        return 42                                 // coverage: FULL
    }
}

object UnusedObject {
    fun boo() {
    } // coverage: NONE
}

class SimpleClassWithDefaultConstructor {         // coverage: NONE
    companion object {
        fun staticFoo() {
            println()                             // coverage: FULL
        }
    }
}

fun main() {
    Util.foo1()                                   // coverage: FULL
    SimpleClassWithDefaultConstructor.staticFoo() // coverage: FULL
}
