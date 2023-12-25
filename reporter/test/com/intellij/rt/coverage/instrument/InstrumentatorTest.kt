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
package com.intellij.rt.coverage.instrument

import com.intellij.rt.coverage.instrument.api.OfflineInstrumentationApi
import com.intellij.rt.coverage.report.TestUtils
import com.intellij.rt.coverage.report.api.Filters
import com.intellij.rt.coverage.util.ClassNameUtil
import org.jetbrains.coverage.org.objectweb.asm.*
import org.junit.Assert
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class InstrumentatorTest {

    @Test
    fun test() {
        val (roots, outputRoots) = createInstrumentatorTask()
        val filters = Filters.EMPTY
        runInstrumentator(roots, outputRoots, filters)
    }

    @Test
    fun apiTest() {
        val (roots, outputRoots) = createInstrumentatorTask()
        val filters = Filters.EMPTY

        TestUtils.clearLogFile(File("."))
        OfflineInstrumentationApi.instrument(roots, outputRoots, filters, true)
        TestUtils.checkLogFile(File("."))
        checkOfflineInstrumentation(roots, outputRoots, filters)
    }

    companion object {
        fun runInstrumentator(roots: List<File>, outputRoots: List<File>, filters: Filters) {
            val inst = Instrumentator(roots, outputRoots, filters)
            TestUtils.clearLogFile(File("."))
            inst.instrument(false)
            TestUtils.checkLogFile(File("."))
            checkOfflineInstrumentation(roots, outputRoots, filters)
        }

        fun createInstrumentatorTask(): Pair<List<File>, List<File>> {
            val roots = listOf(TestUtils.JAVA_OUTPUT, TestUtils.KOTLIN_OUTPUT).map { File(it) }
            val outputRoots = roots.map { createTempDirectory(it.path.replace(File.separator, "_")).toFile() }
            return roots to outputRoots
        }
    }
}

private fun collectFiles(root: File) = root.walk().map { it.toRelativeString(root) }.toHashSet()

private fun checkOfflineInstrumentation(roots: List<File>, outputRoots: List<File>, filters: Filters) {
    for ((root, outputRoot) in roots.zip(outputRoots)) {
        val original = collectFiles(root)
        val transformed = collectFiles(outputRoot)
        Assert.assertEquals(original, transformed)

        val actuallyTransformed = transformed
            .asSequence()
            .filter { it.endsWith(ClassNameUtil.CLASS_FILE_SUFFIX) }
            .map { ClassNameUtil.convertToFQName(ClassNameUtil.removeClassSuffix(it)) }
            .filterNot { it.startsWith("com.intellij.rt.") }
            .filterNot { ClassNameUtil.matchesPatterns(it, filters.excludeClasses) }
            .filter { filters.includeClasses.isEmpty() || ClassNameUtil.matchesPatterns(it, filters.includeClasses) }
            .map { File(outputRoot, it.replace(".", File.separator) + ClassNameUtil.CLASS_FILE_SUFFIX) }
            .toList()
        Assert.assertTrue(actuallyTransformed.isNotEmpty())
        val hasInstrumentation = actuallyTransformed.any { it.isInstrumented() }
        Assert.assertTrue(hasInstrumentation)
    }
}

private fun File.isInstrumented(): Boolean {
    val bytes = readBytes()
    var hasInstrumentation = false
    var hasCondyInstrumentation = false
    val visitor = object : ClassVisitor(Opcodes.API_VERSION) {
        override fun visitMethod(
            access: Int, name: String?, descriptor: String?,
            signature: String?, exceptions: Array<out String>?
        ): MethodVisitor {
            return object : MethodVisitor(Opcodes.API_VERSION) {
                override fun visitMethodInsn(
                    opcode: Int, owner: String?, name: String?,
                    descriptor: String?, isInterface: Boolean
                ) {
                    if (owner == "com/intellij/rt/coverage/offline/RawProjectInit"
                        && name == "getOrCreateHitsMask"
                        && descriptor == "(Ljava/lang/String;I)[I"
                    ) {
                        hasInstrumentation = true
                    }
                }

                override fun visitLdcInsn(value: Any?) {
                    if (value is ConstantDynamic && value.name == "__\$hits\$__") {
                        hasCondyInstrumentation = true
                    }
                }
            }
        }
    }
    ClassReader(bytes).accept(visitor, ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG)
    check(!(hasInstrumentation && hasCondyInstrumentation))
    return hasInstrumentation || hasCondyInstrumentation
}
