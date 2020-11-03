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

package kotlinTestData.whenMapping.tracing

enum class F {
    A, B, C
}

fun f(f: F): Int {
    return when (f) {                   // coverage: PARTIAL as branch 2 not covered
        F.A, F.B -> 42                  // coverage: FULL
        F.C -> 36                       // coverage: NONE
    }
}

enum class SimpleEnum {
    Single
}

fun simpleF(v: SimpleEnum) =
        when (v) {                      // coverage: FULL as no branching for else
            SimpleEnum.Single -> 42     // coverage: FULL
        }                               // coverage: FULL as return is here

fun noneF(v: SimpleEnum) =
        when (v) {                      // coverage: NONE
            SimpleEnum.Single -> 42     // coverage: NONE
        }                               // coverage: NONE

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        f(F.A)
        simpleF(SimpleEnum.Single)
        return
    }
}
