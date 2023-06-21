/*
 * Copyright 2000-2023 JetBrains s.r.o.
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

package testData.fixes.IDEA_323017

// classes: Grader Grader2

interface GraderI {
    fun determineLetterGrade(numberGrade: Int): Char
}

class Grader : GraderI { // coverage: FULL
    override fun determineLetterGrade(numberGrade: Int): Char {
        return when { // coverage: FULL
            numberGrade < 0 ->  // coverage: PARTIAL
                throw IllegalArgumentException("Number Grade cannot be <0")  // coverage: NONE
            numberGrade < 60 -> 'F' // coverage: FULL
            numberGrade < 70 -> 'D' // coverage: FULL
            numberGrade < 80 -> 'C' // coverage: FULL
            numberGrade < 90 -> 'B' // coverage: FULL
            else -> 'A'  // coverage: FULL
        }
    }
}

class Grader2 : GraderI { // coverage: FULL
    override fun determineLetterGrade(numberGrade: Int): Char {
        return when { // coverage: FULL
            numberGrade < 0 -> throw IllegalArgumentException("Number Grade cannot be <0")  // coverage: PARTIAL
            numberGrade < 60 -> 'F' // coverage: FULL
            numberGrade < 70 -> 'D' // coverage: FULL
            numberGrade < 80 -> 'C' // coverage: FULL
            numberGrade < 90 -> 'B' // coverage: FULL
            else -> 'A'  // coverage: FULL
        }
    }
}

fun main() {
    for (grader in listOf(Grader(), Grader2())) {
        for (x in listOf(0, 59, 69, 79, 89, 99)) {
            grader.determineLetterGrade(x)
        }
    }
}
