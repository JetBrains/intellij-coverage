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

package com.intellij.rt.coverage.build

import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import proguard.gradle.ProGuardTask

class ArtifactBuilder {

  static def setUpArtifact(Project project, String artifactName, List<String> extraDependencyModules, Closure<Jar> configureJar = null) {
    setUpArtifactInternal(project, artifactName, extraDependencyModules, configureJar, null)
  }

  private static def setUpArtifactInternal(Project project, String artifactName, List<String> dependencyModules,
                                           Closure<Jar> configureJar, String alternativeJarTask) {
    def modules = dependencyModules.collect({ project.project(it).sourceSets.main })
    modules.add(project.sourceSets.main)

    def jarConfiguration = { Jar jar ->
      jar.archiveBaseName.set(artifactName)
      jar.setDuplicatesStrategy(DuplicatesStrategy.FAIL)
      if (alternativeJarTask == null) {
        jar.destinationDirectory.set(new File(project.rootDir, "dist"))
      }
      if (configureJar != null) configureJar(jar)
      jar.from { project.sourceSets.main.output }
      for (String module in dependencyModules) {
        def jarTask = (Jar) project.project(module).tasks.named("jar").get()
        jar.dependsOn(jarTask)
        jar.from(project.zipTree(jarTask.archivePath))
      }
    }

    if (alternativeJarTask == null) {
      project.tasks.named("jar", Jar, jarConfiguration)
    } else {
      project.tasks.register(alternativeJarTask, Jar, jarConfiguration)
    }

    project.tasks.register("soursesJar", Jar) {
      dependsOn(project.getTasksByName("classes", false))
      archiveBaseName = artifactName
      archiveClassifier = 'sources'
      from(modules.collect { it.allSource })
    }

    project.tasks.register("allJavadoc", Javadoc) {
      options.tags = ["noinspection"]
      source = modules.collect { it.allSource }
      classpath = project.files(modules.collect { it.compileClasspath })
    }

    project.tasks.register("javadocJar", Jar) {
      archiveBaseName = artifactName
      archiveClassifier = 'javadoc'
      from project.getTasksByName("allJavadoc", false)
    }
  }

  private static def setUpProguard(Project project, Jar fullJar, String fullName, String name) {
    project.tasks.register('proguard', ProGuardTask) {
      dependsOn(fullJar)

      def javaHome = System.getProperty('java.home')
      if (System.getProperty('java.version').startsWith('1.')) {
        libraryjars("${javaHome}/lib/rt.jar")
      } else {
        libraryjars(["java.base.jmod", "java.instrument.jmod", "java.xml.jmod"].collect { "${javaHome}/jmods/${it}" })
      }

      keep('public class com.intellij.rt.coverage.** { *; }')
      keepclassmembers('class * { public protected *; }')
      keepattributes()
      keepnames('class ** { *; }')

      def fullPath = fullJar.archiveFile.get().asFile.absolutePath
      def compressedPath = fullPath.replace(fullName, name)
      injars(fullPath)
      outjars(compressedPath)
    }
  }

  static def setUpArtifactWithProguard(Project project, String artifactName,
                                       List<String> dependencyModules,
                                       Closure<Jar> configureJar = null) {
    def proguardEnabled = project.rootProject.hasProperty("coverage.proguard.enable") ? "true" == project.rootProject["coverage.proguard.enable"] : true
    if (!proguardEnabled) {
      setUpArtifact(project, artifactName, dependencyModules, configureJar)
      return
    }
    def fullName = "full-$artifactName"
    def compressedName = "compressed-$artifactName"
    def fullJarTaskName = "fullJar"
    setUpArtifactInternal(project, fullName, dependencyModules, configureJar, fullJarTaskName)

    def fullJarTask = (Jar) project.tasks.named(fullJarTaskName).get()
    setUpProguard(project, fullJarTask, fullName, compressedName)
    project.tasks.named("jar", Jar) {
      dependsOn("proguard")
      archiveBaseName.set(artifactName)
      destinationDirectory.set(new File(project.rootDir, "dist"))
      // do not copy original class files
      mainSpec.sourcePaths.clear()
      from {
        def compressedJar = fullJarTask.archiveFile.get().asFile.absolutePath.replace(fullName, compressedName)
        project.zipTree(compressedJar)
      }
      if (configureJar != null) configureJar(it)
    }
  }
}
