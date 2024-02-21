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

package testData.custom.threadSafe.structure

import org.jetbrains.coverage.org.objectweb.asm.ClassWriter
import org.jetbrains.coverage.org.objectweb.asm.Label
import org.jetbrains.coverage.org.objectweb.asm.Opcodes
import kotlin.system.exitProcess

object F {
    @Volatile
    var x = 0

    @JvmStatic
    fun f() {
        ++x
    }
}

/**
 * Generate indexed classes with simple body.
 */
private object GeneratorClassLoader : ClassLoader() {
    private val PACKAGE_NAME = GeneratorClassLoader::class.java.`package`.name
    val BASE_NAME = "$PACKAGE_NAME.Class"
    override fun loadClass(name: String?): Class<*>? = if (name != null && name.startsWith(BASE_NAME)) {
        val generated = generate(name.replace('.', '/'))
        defineClass(name, generated, 0, generated.size)
    } else {
        super.loadClass(name)
    }

    private fun generate(name: String): ByteArray {
        val cw = ClassWriter(0)
        cw.visit(49, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null)

        val mv1 = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)
        mv1.visitVarInsn(Opcodes.ALOAD, 0)
        mv1.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        mv1.visitInsn(Opcodes.RETURN)
        mv1.visitMaxs(1, 1)
        mv1.visitEnd()

        val mv2 = cw.visitMethod(Opcodes.ACC_PUBLIC, "foo", "()V", null, null)
        val label = Label()
        mv2.visitLabel(label)
        val line = 1 + name.substring(BASE_NAME.length).toInt()
        mv2.visitLineNumber(line, label)
        mv2.visitMethodInsn(Opcodes.INVOKESTATIC, "${PACKAGE_NAME.replace('.', '/')}/F", "f", "()V", false)
        mv2.visitInsn(Opcodes.RETURN)
        mv2.visitMaxs(1, 1)
        mv2.visitEnd()

        cw.visitEnd()
        return cw.toByteArray()
    }
}

const val THREAD_SAFE_STRUCTURE_CLASSES = 100
private const val THREADS = 4

private val tasks = List(THREADS) { iThread ->
    Runnable {
        runCatching {
            repeat(THREAD_SAFE_STRUCTURE_CLASSES / THREADS) { iTask ->
                val iClass = iThread * THREAD_SAFE_STRUCTURE_CLASSES / THREADS + iTask
                val clazz = GeneratorClassLoader.loadClass("${GeneratorClassLoader.BASE_NAME}$iClass")!!
                val instance = clazz.getConstructor().newInstance()
                val method = clazz.getMethod("foo")
                repeat(10) {
                    method.invoke(instance)
                }
            }
        }.onFailure { it.printStackTrace(System.err); exitProcess(1) }
    }
}

fun main() {
    val threads = tasks.map { Thread(it) }
    threads.forEach(Thread::start)
    threads.forEach(Thread::join)
}
