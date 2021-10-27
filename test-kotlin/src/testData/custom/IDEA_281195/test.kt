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

package testData.custom.IDEA_281195

import org.junit.runner.JUnitCore

// classes: MyService MyService$SimpleEmail

class MyService {                       // coverage: FULL
    fun doBusinessOperationXyz() {
        sendNotificationEmail()         // coverage: FULL
    }

    private fun sendNotificationEmail() {
        val email = SimpleEmail()       // coverage: FULL
        email.send()                    // coverage: FULL
    }

    class SimpleEmail {                 // coverage: PARTIAL
        fun send() {
            println("The email is sent.") // coverage: NONE this method is mocked
        }
    }
}

fun main() {
    val result = JUnitCore().run(MyServiceTest::class.java)
    result.failures.firstOrNull()?.also { throw it.exception }
}
