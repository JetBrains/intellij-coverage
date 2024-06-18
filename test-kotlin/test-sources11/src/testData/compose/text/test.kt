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

package testData.compose.text

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runDesktopComposeUiTest

@Composable
fun Greeting(name: String) {  // coverage: FULL
    Text(                     // coverage: FULL
        text = "Hello $name!" // coverage: FULL
    )
}                             // coverage: FULL

@OptIn(ExperimentalTestApi::class)
fun main() {
    runDesktopComposeUiTest {           // coverage: FULL
        setContent {                    // coverage: FULL
            Greeting("xx")              // coverage: FULL
        }
    }
}