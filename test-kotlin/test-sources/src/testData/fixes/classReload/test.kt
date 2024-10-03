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

package testData.fixes.classReload

// classes: A

class A {  // coverage: FULL
    fun foo() {
        val x = 1 // coverage: FULL
    } // coverage: FULL

    fun boo() {
        val x = 1 // coverage: FULL
    } // coverage: FULL
}

class CustomClassLoader(private val bytes: ByteArray) : ClassLoader() {
    override fun loadClass(name: String?): Class<*> {
        if (name != "testData.fixes.classReload.A") return super.loadClass(name)
        return findClass(name)
    }
    override fun findClass(name: String?): Class<*> {
        if (name != "testData.fixes.classReload.A") return super.findClass(name)
        return defineClass(name, bytes, 0, bytes.size)
    }
}

fun main() {
    val bytes = CustomClassLoader::class.java.classLoader.getResourceAsStream("testData/fixes/classReload/A.class")!!.readBytes()

    val cl1 = CustomClassLoader(bytes)
    val cl2 = CustomClassLoader(bytes)

    // Call constructor and foo in both class loaders
    val clazz1 = cl1.loadClass("testData.fixes.classReload.A")
    val instance1 = clazz1.newInstance()
    clazz1.getMethod("foo").invoke(instance1)

    // Reload class
    val clazz2 = cl2.loadClass("testData.fixes.classReload.A")
    val instance2 = clazz2.newInstance()
    clazz2.getMethod("foo").invoke(instance2)

    // Call boo only in the first class loader
    clazz1.getMethod("boo").invoke(instance1)
}
