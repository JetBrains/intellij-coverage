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

package kotlinTestData.properties.file


private const val x = 42   // invisible as property is const
private val x2 = 42        // coverage: FULL as value is written in <cinit>
val y: List<Int>           // coverage: NONE as getter and setter are uncovered
        = ArrayList()      // coverage: FULL as value is written in <cinit>

private val z              // invisible as property is private
        = ArrayList<Int>() // coverage: FULL as value is written in <cinit>


object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        Class.forName("kotlinTestData.properties.file.TestKt")
        return
    }
}
