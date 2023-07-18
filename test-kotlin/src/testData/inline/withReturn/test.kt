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

package testData.inline.withReturn

fun main() {
    MyClass().f { testF() } // coverage: FULL
    MyClass().f2(fun() { testF() }) // coverage: FULL
}

class MyClass {             // coverage: FULL
    inline fun f(f1Param: () -> Unit): MyClass {
        testF()             // coverage: FULL
        f1Param()           // coverage: FULL
        return this         // coverage: FULL this line is inlined as NOP as value is not used
    }

    inline fun f2(f1Param: () -> Unit): MyClass {
        testF()             // coverage: FULL
        f1Param()           // coverage: FULL this line is inlined as NOP in 1.5
        return this         // coverage: FULL this line is inlined as NOP as value is not used
    }
}

fun testF() {} // coverage: FULL
