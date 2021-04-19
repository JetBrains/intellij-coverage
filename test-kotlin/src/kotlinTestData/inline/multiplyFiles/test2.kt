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

package kotlinTestData.inline.multiplyFiles

import kotlin.random.Random


inline fun separateA(f: (Int) -> Unit, gen: () -> Int) {
    val x = gen()                // coverage: FULL
    print("Got x = ")            // coverage: FULL
    f(x)                         // coverage: FULL
}                                // coverage: FULL

inline fun separateC(f: (x: Int) -> Unit) {
    separateA(f) {               // coverage: FULL
        Random.nextInt()         // coverage: FULL
    }
}                                // coverage: FULL
