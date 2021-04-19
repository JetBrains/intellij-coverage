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

package kotlinTestData.inline.lambda

import kotlin.random.Random


private inline fun a(f: (Int) -> Unit, gen: () -> Int) {
    val x = gen()                // coverage: FULL
    print("Got x = ")            // coverage: FULL
    f(x)                         // coverage: FULL
}                                // coverage: FULL

private inline fun c(f: (x: Int) -> Unit) {
    a(f) {                       // coverage: FULL
        Random.nextInt()         // coverage: FULL
    }
}                                // coverage: FULL

private fun f() {
    c {                          // coverage: FULL
        println(it)              // coverage: FULL
    }                            // coverage: FULL
}

private inline fun b(
        list: List<Int>,
        foo: (x: Int) -> Int
) = list.map(foo)                // coverage: FULL

fun test() {
    b(listOf(3, 4)) {            // coverage: FULL
        it + 45                  // coverage: FULL
    }
    f()                          // coverage: FULL
}

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        test()
    }
}
