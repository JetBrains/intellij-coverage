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

package testData.enum_.kotlin

// classes: MyEnum MyUnloadedEnum
// calculate unloaded: true

// Kotlin 1.4 does not include LINENUMBER labels into the <clinit> method,
// so there are no lines presented. Kotlin 1.5 performs the same as Java.

enum class MyEnum {
    A,
    B,
    C;
}

enum class MyUnloadedEnum {
    A,
    B,
    C;
}

fun main() {
    MyEnum.A
}
