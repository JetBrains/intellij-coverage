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

package testData.custom.offline.api.basicSwitch

import testData.custom.offline.api.collectCoverageAndDump

fun foo(x: String) = when (x) {
    "a", "b" -> println(5)
    "c" -> println(6)
    else -> println(7)
}


fun foo2(x: String?) = when (x) {
    "a", "b" -> println(5)
    "c" -> println(6)
    else -> println(7)
}

fun main() {
    foo("a")
    foo("b")

    foo2("a")
    foo2("b")
    foo2("c")
    foo2("d")

    collectCoverageAndDump()
}
