import org.gradle.internal.jvm.Jvm

pluginManagement {
    val kotlinVersion: String? by settings
    val versionOrDefault = kotlinVersion ?: "2.0.0"
    val composeVersion = if (versionOrDefault.startsWith("1.8")) "1.6.0" else "1.6.11"
    plugins {
        id("org.jetbrains.kotlin.jvm") version versionOrDefault
        id("org.jetbrains.compose") version composeVersion
    }
}

rootProject.name = "intellij-coverage"

include(":instrumentation")
include(":instrumentation:java7-utils")
include(":test-discovery")
include(":junit4-test-discovery-launcher")
include(":tests")
include(":util")
include(":reporter")
include(":reporter:offline")
include(":benchmarks")
include(":java6-utils")
include(":offline-runtime")
include(":offline-runtime:data")
include(":offline-runtime:java7-utils")
include(":offline-runtime:api")
include(":common")
include(":lib")

include(":test-kotlin")
include(":test-kotlin:test-utils")
include(":test-kotlin:test-generation")
include(":test-kotlin:test-sources")
if (Jvm.current().javaVersion!! >= JavaVersion.VERSION_11) {
    include(":test-kotlin:test-sources11")
}
