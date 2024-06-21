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

package testData.basicJava.tryWithResources.kotlin

fun main() {
    val closable = object : AutoCloseable { // coverage: FULL
        override fun close() {
            println("Close") // coverage: FULL
        }
    }

    closable.use { // coverage: FULL
        println("Use resource") // coverage: FULL
    }              // coverage: FULL
}
