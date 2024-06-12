import org.gradle.internal.jvm.Jvm
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.8.21"
    id("org.jetbrains.compose") apply false
}

subprojects {
    apply(plugin = "kotlin")
    val isJava11 = Jvm.current().javaVersion!! >= JavaVersion.VERSION_11
            && rootProject.properties["test.configuration"] == "true"
    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = if (isJava11) JavaVersion.VERSION_11 else JavaVersion.VERSION_1_8
    }
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = if (isJava11) "11" else "1.8"
            val kotlinVersion: String? by project
            if (kotlinVersion?.startsWith("1.5") != true) {
                freeCompilerArgs += "-Xlambdas=indy"
            }
        }
    }
    sourceSets {
        main.configure {
            java.srcDir("src")
        }
        test.configure {
            java.srcDir("test")
            resources.srcDir("resources")
        }
    }
    tasks.withType<Test>().configureEach {
        val fastTests = rootProject.properties["fast.tests"]
        if (fastTests == "true") {
            jvmArgs("-Dcoverage.run.fast.tests=true")
        }
    }
}

