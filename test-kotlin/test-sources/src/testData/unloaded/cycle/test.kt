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

package testData.unloaded.cycle

// classes: User
// calculate unloaded: true

fun main() {
}

class User(val actions: Int) {                      // coverage: NONE 
    fun main() {
        val activeUsers = mutableListOf<User>()     // coverage: NONE 
        val inactiveUsers = mutableListOf<User>()   // coverage: NONE 

        val actionsThreshold = 4                    // coverage: NONE 
        val users = listOf(User(5))                 // coverage: NONE 
        for (user in users) {                       // coverage: NONE // branches: 0/2
            if (user.actions < actionsThreshold) {  // coverage: NONE // branches: 0/2
                inactiveUsers.add(user)             // coverage: NONE 
            } else {
                activeUsers.add(user)               // coverage: NONE 
            }
        }
    }
}

