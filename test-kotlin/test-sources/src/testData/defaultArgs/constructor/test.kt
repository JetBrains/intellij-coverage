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

package testData.defaultArgs.constructor

// classes: Example1 Example2 Example3 Example4 OverloadExample1 OverloadExample2 OverloadExample3 OverloadExample4
// instructions & branches
// calculate unloaded: true

class Example1(name: String = "foo") // coverage: FULL // stats: 11/11
class Example2(name: String = "foo") // coverage: FULL // stats: 11/11
class Example3(name: String = "foo") // coverage: FULL // stats: 11/11
class Example4(name: String = "foo") // coverage: NONE // stats: 0/11

class OverloadExample1 @JvmOverloads constructor(name: String = "foo") // coverage: FULL // stats: 11/11
class OverloadExample2 @JvmOverloads constructor(name: String = "foo") // coverage: FULL // stats: 11/11
class OverloadExample3 @JvmOverloads constructor(name: String = "foo") // coverage: FULL // stats: 11/11
class OverloadExample4 @JvmOverloads constructor(name: String = "foo") // coverage: NONE // stats: 0/11



fun main() {
    Example1()
    Example1("boo")

    Example2()

    Example3("boo")

    OverloadExample1()
    OverloadExample1("boo")

    OverloadExample2()

    OverloadExample3("boo")
}