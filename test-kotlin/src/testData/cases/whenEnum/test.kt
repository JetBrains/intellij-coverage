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

package testData.cases.whenEnum

// with branches

enum class F {
    A, B, C // coverage: FULL
}

fun f(f: F): Int {
    return when (f) {           // coverage: PARTIAL // branches: 1/3
        F.A, F.B -> 42          // coverage: FULL
        F.C -> 36               // coverage: NONE
    }
}

enum class SimpleEnum {
    Single // coverage: FULL // stats: 8/8
}

fun simpleF(v: SimpleEnum) =
    when (v) {                  // coverage: FULL
        SimpleEnum.Single -> 42 // coverage: FULL // branches: 2/2
    }

fun noneF(v: SimpleEnum) =
    when (v) {                  // coverage: NONE
        SimpleEnum.Single -> 42 // coverage: NONE // branches: 0/2
    }

fun main() {
    f(F.A)                      // coverage: FULL
    simpleF(SimpleEnum.Single)  // coverage: FULL
}
