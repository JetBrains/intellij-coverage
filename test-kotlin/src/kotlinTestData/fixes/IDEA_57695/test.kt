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

package kotlinTestData.fixes.IDEA_57695

import kotlinTestData.fixes.IDEA_57695.GeneratorClassLoader.foo
import org.jetbrains.annotations.NotNull
import org.jetbrains.coverage.org.objectweb.asm.ClassWriter
import org.jetbrains.coverage.org.objectweb.asm.Label
import org.jetbrains.coverage.org.objectweb.asm.Opcodes

/**
 * This loader generates bytecode equivalent to [foo].
 */
private object GeneratorClassLoader : ClassLoader() {
    const val BASE_NAME = "kotlinTestData.fixes.IDEA_57695"
    const val METHOD_NAME = "foo"
    private const val REPORT_NULL_NAME = "\$\$\$reportNull\$\$\$"
    private const val REPORT_NULL_SIGNATURE = "(I)V"
    private const val OBJECT = "java/lang/Object"

    @NotNull
    fun foo(x: Any?): Any? {
        return if (x == null)
            null else x
    }

    override fun loadClass(name: String?): Class<*>? = if (name != null && name.startsWith(BASE_NAME)) {
        val generated = generate(name.replace('.', '/'))
        defineClass(name, generated, 0, generated.size)
    } else {
        super.loadClass(name)
    }

    private fun generate(name: String): ByteArray {
        val cw = ClassWriter(0)
        cw.visit(49, Opcodes.ACC_PUBLIC, name, null, OBJECT, null)

        val mv1 = cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, REPORT_NULL_NAME, REPORT_NULL_SIGNATURE, null, null)
        mv1.visitInsn(Opcodes.RETURN)
        mv1.visitMaxs(1, 1)
        mv1.visitEnd()

        val mv2 = cw.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, METHOD_NAME, "(L$OBJECT;)L$OBJECT;", null, null)
        val line1 = Label()
        mv2.visitLabel(line1)
        mv2.visitLineNumber(1, line1)

        mv2.visitVarInsn(Opcodes.ALOAD, 0)
        val ifLabel = Label()
        val returnOnLine1 = Label()
        mv2.visitJumpInsn(Opcodes.IFNULL, ifLabel)

        val line2 = Label()
        mv2.visitLabel(line2)
        mv2.visitLineNumber(2, line2)

        mv2.visitVarInsn(Opcodes.ALOAD, 0)
        mv2.visitJumpInsn(Opcodes.GOTO, returnOnLine1)

        mv2.visitLabel(ifLabel)
        mv2.visitInsn(Opcodes.ACONST_NULL)

        mv2.visitLabel(returnOnLine1)
        mv2.visitLineNumber(1, returnOnLine1)

        mv2.visitInsn(Opcodes.DUP)
        val returnLabel = Label()
        mv2.visitJumpInsn(Opcodes.IFNONNULL, returnLabel)
        mv2.visitInsn(Opcodes.ICONST_0)
        mv2.visitMethodInsn(Opcodes.INVOKESTATIC, name, REPORT_NULL_NAME, REPORT_NULL_SIGNATURE, false)

        mv2.visitLabel(returnLabel)
        mv2.visitInsn(Opcodes.ARETURN)
        mv2.visitMaxs(2, 1)
        mv2.visitEnd()

        cw.visitEnd()
        return cw.toByteArray()
    }
}

fun test() {
    val clazz = GeneratorClassLoader.loadClass("${GeneratorClassLoader.BASE_NAME}.TestClass")!!
    clazz.getMethod(GeneratorClassLoader.METHOD_NAME, Any::class.java).invoke(null, Any())
}

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        test()
    }
}
