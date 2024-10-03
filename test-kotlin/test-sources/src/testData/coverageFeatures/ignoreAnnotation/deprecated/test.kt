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

package testData.coverageFeatures.ignoreAnnotation.deprecated

// calculate unloaded: true

@Deprecated("", level = DeprecationLevel.WARNING)
fun deprecatedFunction1() {
    println() // coverage: NONE
} // coverage: NONE

@Deprecated("", level = DeprecationLevel.ERROR)
fun deprecatedFunction2() {
    println()
}

@Deprecated("", level = DeprecationLevel.HIDDEN)
fun deprecatedFunction3() {
    println()
}

@Deprecated("", level = DeprecationLevel.HIDDEN)
fun deprecatedFunctionWithDefaultArgs(
        x: Int = 42
) {
    println()
}

@Deprecated("", level = DeprecationLevel.HIDDEN)
fun deprecatedFunctionWithAnonymousClass() {
    functionWithLambda {
        it + 42
    }

    functionWithLambda {
        functionWithLambda {
            functionWithLambda {
                it + 42
            }
            it + 42
        }
        it + 42
    }

    object : Foo {
        override fun foo() {
            println()
        }
    }
}

object A {
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    fun deprecatedFunctionWithAnonymousClass() {
        functionWithLambda {
            it + 42
        }

        functionWithLambda {
            functionWithLambda {
                functionWithLambda {
                    it + 42
                }
                it + 42
            }
            it + 42
        }

        object : Foo {
            override fun foo() {
                println()
            }
        }
    }

    fun foo() = Unit // coverage: NONE
}

fun functionWithLambda(lambda: (Int) -> Int) {
    print(lambda(5)) // coverage: NONE
} // coverage: NONE

interface Foo {
    fun foo()
}

fun main() {
} // coverage: FULL
