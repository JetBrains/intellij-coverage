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

package testData.simple.branches

// classes: MyBranchedClass
// instructions & branches

class MyBranchedClass {           // coverage: FULL // stats: 2/2
    fun foo(value: Int) {
        if (value < 0) {          // coverage: PARTIAL // stats: 3/3 1/2
            println("LE")         // coverage: FULL    // stats: 3/3
        } else if (value == 0) {  // coverage: NONE    // stats: 0/3 0/2
            println("EQ")         // coverage: NONE    // stats: 0/4
        } else {
            println("GE")         // coverage: NONE    // stats: 0/2
        }
    }
}

fun main() {
    MyBranchedClass().foo(-20)
}
