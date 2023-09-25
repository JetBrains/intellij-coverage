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

package testData.custom.api.intellij

import com.intellij.rt.coverage.data.ClassData
import com.intellij.rt.coverage.data.LineData
import com.intellij.rt.coverage.data.ProjectData
import com.intellij.rt.coverage.instrumentation.UnloadedUtil
import com.intellij.rt.coverage.report.XMLCoverageReport
import com.intellij.rt.coverage.report.XMLProjectData
import com.intellij.rt.coverage.util.CoverageReport
import com.intellij.rt.coverage.util.ProjectDataLoader

// In this test we just test that all classes that are requites by IntelliJ are present in the artifact
fun main() {
    // data classes
    val projectData = ProjectData()
    val classData = ClassData("x")
    val lineData = LineData(5, "()V")

    // report loading
    ProjectDataLoader::class.java
    CoverageReport::class.java
    UnloadedUtil::class.java

    // xml
    val xmlProjectData = XMLProjectData()
    XMLCoverageReport::class.java
}
