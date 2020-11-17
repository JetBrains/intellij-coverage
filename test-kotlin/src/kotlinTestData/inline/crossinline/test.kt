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

package kotlinTestData.inline.crossinline


private var counter = 0
fun logAndDo(s: String, operation: (String) -> Unit) {
    println("Log: \"$s\"")               // coverage: FULL
    operation("${++counter} $s")         // coverage: FULL
}                                        // coverage: FULL

val list = mutableListOf<String>()       // coverage: FULL
inline fun save(message: String, crossinline foo: (String) -> Unit) {
    logAndDo(message) { loggedMessage -> // coverage: FULL
        list.add(loggedMessage)          // coverage: FULL
        foo(loggedMessage)               // coverage: FULL
    }                                    // coverage: FULL
}                                        // coverage: FULL

fun test() {
    save("Hello") {                      // coverage: FULL
        println("\"$it\" saved")         // coverage: FULL
    }                                    // coverage: FULL
}                                        // coverage: FULL

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        test()
    }
}
