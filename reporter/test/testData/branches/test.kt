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

class MyBranchedClass {           
    fun foo(value: Int) {
        if (value < 0) {          
            println("LE")         
        } else if (value == 0) {  
            println("EQ")         
        } else {
            println("GE")         
        }
    }

    fun boo(value: Int) {
        when (value) {
            2 -> println(1)
            3 -> println(2)
            4 -> println(3)
            else -> println(4)
        }
    }
}

class MyBranchedUnloadedClass {
    fun foo(value: Int) {
        if (value < 0) {
            println("LE")
        } else if (value == 0) {
            println("EQ")
        } else {
            println("GE")
        }
    }

    fun boo(value: Int) {
        when (value) {
            2 -> println(1)
            3 -> println(2)
            4 -> println(3)
            else -> println(4)
        }
    }
}

fun main() {
    MyBranchedClass().foo(-20)
}
