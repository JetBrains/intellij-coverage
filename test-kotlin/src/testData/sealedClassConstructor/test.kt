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

package testData.sealedClassConstructor

sealed class SealedClass                      // coverage: FULL

sealed class SealedClassWithArgs(private val x: Int)                          // coverage: FULL

data class Derived(private val x: Int) : SealedClass()           // coverage: FULL

data class Derived2(private val x: Int) : SealedClassWithArgs(x) // coverage: FULL

class ClassWithPrivateDefaultConstructor private constructor(val x: Int) { // coverage: FULL
    constructor(x: String) : this(x.toInt())                     // coverage: FULL
}

fun main() {
    Derived(5)                                                   // coverage: FULL
    Derived2(10)                                                 // coverage: FULL
    ClassWithPrivateDefaultConstructor("42").x                   // coverage: FULL
}
