/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

package kotlinTestData.inline.reflection

object Test {                    // coverage: FULL
    @JvmStatic
    fun main(args: Array<String>) {
        test()                   // coverage: FULL
    }

    inline fun a(x: Int) {
        println(x)                // coverage: FULL
    }

    inline fun b(f: (Int) -> Int) {
        println(f(4))             // coverage: FULL
    }

    fun test() {
        this::class.java.getDeclaredMethod("a", Int::class.java).invoke(this, 42)   // coverage: FULL
        val f =  { x: Int -> x + 42 }                                               // coverage: FULL
        this::class.java.declaredMethods.single { it.name == "b" }.invoke(this, f)  // coverage: FULL
    }
}
