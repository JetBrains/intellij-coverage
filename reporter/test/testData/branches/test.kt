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

package testData.branches

// classes: MyBranchedClass

class MyBranchedClass {           // coverage: FULL
    fun foo(value: Int) {
        if (value < 0) {          // coverage: PARTIAL
            println("LE")         // coverage: FULL
        } else if (value == 0) {  // coverage: NONE
            println("EQ")         // coverage: NONE
        } else {
            println("GE")         // coverage: NONE
        }
    }
}

fun main() {
    MyBranchedClass().foo(-20)
}
