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

package testData.custom.offline.api

import com.intellij.rt.coverage.offline.api.CoverageRuntime
import java.io.File
import java.nio.file.Paths

private fun getPackageName(): String {
    val e = Throwable()
    val className = e.stackTrace[2].className
    val i = className.lastIndexOf('.')
    return if (i >= 0) className.substring(0, i) else ""
}

internal fun collectCoverageAndDump() {
    val resultPath = System.getProperty("coverage.test.result.path") ?: error("No path specified to test (coverage.test.result.path)")
    val classFiles = collectClassFiles(getPackageName())
    val classes = CoverageRuntime.collectClassfileData(classFiles)

    File(resultPath).printWriter().use { out ->
        for (clazz in classes) {
            out.println("Class ${clazz.className} in ${clazz.fileName}:")
            for (method in clazz.methods) {
                out.println("  Method ${method.signature} hits=${method.hits}:")
                for (line in method.lines) {
                    out.println("    Line ${line.lineNumber} hits=${line.hits}")
                    val branches = line.branchHits
                    if (branches.isNotEmpty()) {
                        out.println("      Branches: ${branches.joinToString()}")
                    }
                }
            }
            out.println()
        }
    }

}

fun collectClassFiles(packageName: String): List<ByteArray> {
    val parts = mutableListOf("build", "classes", "kotlin", "main")
    parts.addAll(packageName.split('.'))
    val packageRoot = Paths.get("test-sources", *parts.toTypedArray()).toFile()
    check(packageRoot.exists()) { "Incorrect class files root $packageRoot" }
    return packageRoot.walk()
        .filter { it.isFile }
        .map { it.readBytes() }
        .toList()
}
