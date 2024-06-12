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

package testData.compose.basic

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*

@Composable
fun Counter() {                                 // coverage: FULL 
    var count by remember { mutableStateOf(0) } // coverage: FULL 
    Button(onClick = {                          // coverage: FULL 
        count += 1                              // coverage: FULL 
    }) {                                        // coverage: FULL 
        Text("count = $count")                  // coverage: FULL 
    }
}                                               // coverage: FULL

@Composable
@Preview
fun App() {                                                  // coverage: FULL 
    var text by remember { mutableStateOf("Hello, World!") } // coverage: FULL 

    MaterialTheme {                                         // coverage: FULL 
        Button(onClick = {                                  // coverage: FULL 
            text = "Hello, Desktop!"                        // coverage: NONE 
        }) {                                                // coverage: FULL 
            Text(text)                                      // coverage: FULL 
        }
    }
    Counter()                                               // coverage: FULL 
}                                                           // coverage: FULL

private fun assert(value: Boolean) {
    if (!value) error("Assertion failed") // coverage: PARTIAL // branches: 1/2
}

@OptIn(ExperimentalTestApi::class)
fun main() {
    runDesktopComposeUiTest {           // coverage: FULL 
        setContent {                    // coverage: FULL 
            App()                       // coverage: FULL 
        }
        val textField = onNodeWithText("count = 0")  // coverage: FULL 
        assert(textField.isDisplayed())              // coverage: FULL 
        textField.performClick()                     // coverage: FULL 
        val textField2 = onNodeWithText("count = 1") // coverage: FULL 
        assert(textField2.isDisplayed())             // coverage: FULL 
    }
}
