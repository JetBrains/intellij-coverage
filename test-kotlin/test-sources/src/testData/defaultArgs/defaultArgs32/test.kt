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

package testData.defaultArgs.defaultArgs32

// offline instrumentation

// functions with more than 32 default parameters have more than one extra mask parameter
private fun functionWithSoManyDefaultArguments(
    a01: Int = 1,                  // coverage: FULL
    a02: Int = 1,                  // coverage: FULL
    a03: Int = 1,                  // coverage: FULL
    a04: Int = 1,                  // coverage: FULL
    a05: Int = 1,                  // coverage: FULL
    a06: Int = 1,                  // coverage: FULL
    a07: Int = 1,                  // coverage: FULL
    a08: Int = 1,                  // coverage: FULL
    a09: Int = 1,                  // coverage: FULL
    a10: Int = 1,                  // coverage: FULL
    a11: Int = 1,                  // coverage: FULL
    a12: Int = 1,                  // coverage: FULL
    a13: Int = 1,                  // coverage: FULL
    a14: Int = 1,                  // coverage: FULL
    a15: Int = 1,                  // coverage: FULL
    a16: Long = 1,                 // coverage: FULL
    a17: Int = 1,                  // coverage: FULL
    a18: Int = 1,                  // coverage: FULL
    a19: Int = 1,                  // coverage: FULL
    a20: Int = 1,                  // coverage: FULL
    a21: Int = 1,                  // coverage: FULL
    a22: Int = 1,                  // coverage: FULL
    a23: Int = 1,                  // coverage: FULL
    a24: Int = 1,                  // coverage: FULL
    a25: Int = 1,                  // coverage: FULL
    a26: Int = 1,                  // coverage: FULL
    a27: Int = 1,                  // coverage: FULL
    a28: Int = 1,                  // coverage: FULL
    a29: Int = 1,                  // coverage: FULL
    a30: Int = 1,                  // coverage: FULL
    a31: Int = 1,                  // coverage: FULL
    a32: Int = 1,                  // coverage: FULL (could be PARTIAL AS `if` here compares with next flag variable)
    a33: Long = 1                  // coverage: FULL
): Int {
    return a01 + a02 + a03 + a04 + a05 + a06 + a07 + a08 + a09 + a10 + a11 + a12 + a13 + a14 + a15 + a16.toInt() +      // coverage: FULL
            a17 + a18 + a19 + a20 + a21 + a22 + a23 + a24 + a25 + a26 + a27 + a28 + a29 + a30 + a31 + a32 + a33.toInt() // coverage: FULL
}

fun main() {
    functionWithSoManyDefaultArguments()                                                                                // coverage: FULL
} // coverage: FULL
