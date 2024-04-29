/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package testData.`object`

// calculate unloaded: true

object EmptyObjectDeclaration // coverage: FULL

object ObjectDeclarationWithConstructor {
    init { // coverage: FULL
    }
}

object ObjectDeclarationWithField {
    val x: Int = 42 // coverage: FULL
}

interface I {
    companion object {
        const val X: Int = 42
    }
}

class IC { // coverage: NONE
    companion object {
        const val X: Int = 42
    }
}

object ObjectDeclarationWithConst {
    const val X: Int = 42
}

object ObjectDeclarationWithStringConst {
    const val s: String = "42"
}

object UnusedEmptyObjectDeclaration // coverage: NONE

object UnusedObjectDeclarationWithConstructor {
    init { // coverage: NONE
    }
}

object UnusedObjectDeclarationWithField {
    val x: Int = 42 // coverage: NONE
}

object UnusedObjectDeclarationWithConst {
    const val X: Int = 42
}

object UnusedObjectDeclarationWithStringConst {
    const val s: String = "42"
}


fun main() {
    EmptyObjectDeclaration // coverage: FULL
    ObjectDeclarationWithConstructor // coverage: FULL
    ObjectDeclarationWithField // coverage: FULL
    println(ObjectDeclarationWithConst.X) // coverage: FULL
    println(ObjectDeclarationWithStringConst.s) // coverage: FULL
}
