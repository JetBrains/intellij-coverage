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

package kotlinTestData.newInstrumentation.joda

import junit.textui.TestRunner
import kotlinTestData.newInstrumentation.NullStream
import org.joda.time.TestAllPackages
import java.io.PrintStream
import java.util.*

object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        val original = System.out
        try {
            System.setOut(PrintStream(NullStream()))
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
            Locale.setDefault(Locale("th", "TH"))

            object : TestRunner() {}.start(arrayOf(TestAllPackages::class.java.name))
        } finally {
            System.setOut(original)
        }

    }
}
