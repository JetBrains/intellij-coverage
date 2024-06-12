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

package testData.defaultArgs.severalArguments

// offline instrumentation

private fun functionWithDefaultArguments(
    x: Int = 3,                     // coverage: NONE
    y: Int = 42                     // coverage: FULL
): Int {
    return x + y                    // coverage: FULL
}

fun main() {
    functionWithDefaultArguments(4) // coverage: FULL
}
