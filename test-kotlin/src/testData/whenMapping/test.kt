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

package testData.whenMapping

// classes: ALL
// instructions & branches

enum class F {
    A, B, C
}

fun f(f: F): Int {
    return when (f) {           // coverage: PARTIAL // stats: 5/5 1/3
        F.A, F.B -> 42          // coverage: FULL    // stats: 1/1
        F.C -> 36               // coverage: NONE    // stats: 0/5
    }
}

enum class SimpleEnum {
    Single
}

fun simpleF(v: SimpleEnum) =
    when (v) {                  // coverage: FULL // stats: 5/5 1/1
        SimpleEnum.Single -> 42 // coverage: FULL // stats: 5/5
    }

fun noneF(v: SimpleEnum) =
    when (v) {                  // coverage: NONE // stats: 0/5 0/1
        SimpleEnum.Single -> 42 // coverage: NONE // stats: 0/5
    }

fun main() {
    f(F.A)                      // coverage: FULL // stats: 3/3
    simpleF(SimpleEnum.Single)  // coverage: FULL // stats: 3/3
}
