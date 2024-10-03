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

package testData.defaultArgs.args32

// offline instrumentation

fun emptyFunction() = Unit // coverage: NONE

fun functionWithSoManyParams(
    x00: Int, x01: Int, x02: Int, x03: Int, x04: Int, x05: Int, x06: Int, x07: Int, x08: Int, x09: Int,
    x10: Int, x11: Int, x12: Int, x13: Int, x14: Int, x15: Int, x16: Int, x17: Int, x18: Int, x19: Int,
    x20: Int, x21: Int, x22: Int, x23: Int, x24: Int, x25: Int, x26: Int, x27: Int, x28: Int, x29: Int,
    x30: Int, x31: Int,
    a: Long = 1                       // coverage: FULL
) {
} // coverage: FULL


fun test() {
    functionWithSoManyParams(         // coverage: FULL
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // coverage: FULL
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // coverage: FULL
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // coverage: FULL
        0, 0                          // coverage: FULL
    )
} // coverage: FULL

fun main() {
    test()                            // coverage: FULL
} // coverage: FULL
