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

package testData.redefine

import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.matcher.ElementMatchers.named

// classes: A

fun main() {
    ByteBuddyAgent.install()
    ByteBuddy()
            .redefine(A::class.java)
            .method(named("getValue"))
            .intercept(FixedValue.value(2))
            .make()
            .load(A::class.java.classLoader, ClassReloadingStrategy.fromInstalledAgent())

    val a = A()
    if (a.value != 2) {
        throw RuntimeException("redefinition failed")
    } else {
        println("OK")
    }
}

// not covered, as methods are only called with redefined class
class A {            // coverage: NONE
    val value: Int
        get() = 1    // coverage: NONE
}
