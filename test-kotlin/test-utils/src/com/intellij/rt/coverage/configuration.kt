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

package com.intellij.rt.coverage

enum class Coverage {
    LINE, LINE_FIELD, BRANCH, BRANCH_FIELD, LINE_CONDY, BRANCH_CONDY;

    fun isBranchCoverage() = this == BRANCH || this == BRANCH_FIELD || this == BRANCH_CONDY
    fun isCondyEnabled() = this == LINE_CONDY || this == BRANCH_CONDY

    companion object {
        fun valuesWithCondyWhenPossible() =
            if (getVMVersion() >= 11) values() else values().filterNot { it.isCondyEnabled() }.toTypedArray()
    }
}

enum class TestTracking {
    ARRAY, CLASS_DATA
}

fun getCoverageConfigurations() = if (System.getProperty("coverage.run.fast.tests") != null) {
    listOfNotNull(
        arrayOf(Coverage.BRANCH_FIELD, null),
        arrayOf(Coverage.LINE_CONDY, null).takeIf { getVMVersion() >= 11 },
    ).toTypedArray()
} else {
    allTestTrackingModes()
}

fun allTestTrackingModes() = Coverage.valuesWithCondyWhenPossible().toList()
    .product(TestTracking.values().toList().plus(null))
    .map { it.toList().toTypedArray() }.toTypedArray()

private fun <T, U> Iterable<T>.product(other: Iterable<U>): List<Pair<T, U>> =
    flatMap { l -> other.map { r -> l to r } }

private fun getVMVersion(): Int {
    var version = System.getProperty("java.version")
    if (version.startsWith("1.")) {
        version = version.substring(2, 3)
    } else {
        val dot = version.indexOf(".")
        if (dot != -1) {
            version = version.substring(0, dot)
        }
    }
    return version.toInt()
}
