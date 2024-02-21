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

package testData.implementationByDelegationGeneric

// classes: BDelegation

interface A<T> {
    fun get(): T
}

class BImplementation : A<Int> {
    override fun get() = 42
}

class BDelegation                // coverage: FULL
(): A<Int> by BImplementation()  // coverage: FULL

fun main() {
    BDelegation().get()
    return
}
