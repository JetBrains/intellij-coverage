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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        val kotlinVersion: String? by project
        val versionOrDefault = kotlinVersion ?: "2.0.0"
        if (versionOrDefault.startsWith("2.")) {
            classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:$versionOrDefault")
        }
    }
}

plugins {
    id("org.jetbrains.compose")
}

val kotlinVersion: String? by project
if (kotlinVersion?.startsWith("2.") != false) {
    apply(plugin = "org.jetbrains.kotlin.plugin.compose")
}

repositories {
    google()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(compose.desktop.uiTestJUnit4)
    implementation(compose.desktop.currentOs)

    testImplementation(project(":test-kotlin:test-utils"))
}
