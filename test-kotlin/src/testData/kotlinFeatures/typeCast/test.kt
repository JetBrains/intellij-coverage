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

package testData.kotlinFeatures.typeCast

import kotlin.random.Random
import kotlin.test.assertIs

// classes: TestKt

interface A {
    companion object {
        fun getInstance(name: String): A? = when (name) {
            "B" -> B()
            "C" -> C()
            else -> null
        }
    }
}

class B : A
class C : A

fun f(s: String?): String {
    // no check cast here as type is the same
    return s as String // coverage: FULL
}

fun main() {
    A.getInstance("B") as B         // coverage: FULL
    assertIs<B>(A.getInstance("B")) // coverage: FULL
    f("Hi") // coverage: FULL

    val result: String? = if (Random(2).nextBoolean()) "hello" else null // coverage: PARTIAL
    if (result == null) { // coverage: PARTIAL
        throw NullPointerException("") // coverage: NONE
    }

    val result2: String? = if (Random(2).nextBoolean()) "hello" else null // coverage: PARTIAL
    if (result2 == null) { // coverage: PARTIAL
        throw NullPointerException("null cannot be cast to non-null type kotlin.String") // coverage: NONE
    }
}
