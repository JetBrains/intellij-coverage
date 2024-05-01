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

package testData.classAndInheritanceFilter

import java.io.Closeable

// inheritable types
interface Interface

open class A

// children
class RegularClass {
    fun function() {
        println("function")
    }
}

// direct inheritance of project class
open class B: A() {
    fun functionB() {
        println("function")
    }
}

// direct implementation of project interface
open class C : Interface {
    fun functionC() {
        println("function")
    }
}

// direct implementation of non-project interface
open class D: Closeable {
    fun functionD() {
        println("function")
    }

    override fun close() {
        println("foo")
    }
}

// direct inheritance of project class
class AChild: A() {
    fun functionAA() {
        println("function")
    }
}

// indirect inheritance of project class
class BChild: B() {
    fun functionBB() {
        println("function")
    }
}

// indirect implementation of project interface
class CChild: C() {
    fun functionCC() {
        println("function")
    }
}

// indirect implementation of non-project interface
class DChild: D() {
    fun functionDD() {
        println("function")
    }
}

// indirect implementation of twice indirect non-project interface (AutoCloseable)
class CloseableClass : Closeable {
    fun functionCC() {
        println("function")
    }

    override fun close() {
        println("foo")
    }
}

fun main() {

}
