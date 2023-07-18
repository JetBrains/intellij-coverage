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
import org.gradle.api.publish.maven.MavenPublication

class Publishing {

  static class MyDependency {
    String group
    String name
    String version

    MyDependency(group, name, version) {
      this.group = group
      this.name = name
      this.version = version
    }
  }

  private static def addDependencies(MavenPublication publication, List<MyDependency> dependencyList) {
    publication.pom.withXml {
      def dependencies = asNode().appendNode('dependencies')
      dependencyList.each {
        def dependency = dependencies.appendNode('dependency')
        dependency.appendNode('groupId', it.group)
        dependency.appendNode('artifactId', it.name)
        dependency.appendNode('version', it.version)
      }
    }
  }

  static def setUpPublication(Project root, String projectName, String artifactName, String name, String description, List<MyDependency> extraDependencies = null) {
    root.project(projectName).afterEvaluate { Project proj ->
      root.publishing {
        publications {
          "${projectName.replace(":", "-")}-"(MavenPublication) { publication ->
            publication.artifactId artifactName
            publication.version root.version
            publication.artifact proj.jar
            publication.artifact proj.tasks.named("soursesJar").get()
            publication.artifact proj.tasks.named("javadocJar").get()

            if (extraDependencies != null) addDependencies(publication, extraDependencies)

            Publishing.setMavenMetadata(publication, name, description)
          }
        }
      }
    }
  }

  private static def setMavenMetadata(MavenPublication publication, String publicationName, String publicationDescription) {
    publication.pom {
      name = publicationName
      description = publicationDescription
      url = 'https://github.com/JetBrains/intellij-coverage'
      licenses {
        license {
          name = 'Apache License, Version 2.0'
          url = 'https://www.apache.org/licenses/LICENSE-2.0'
        }
      }
      developers {
        developer {
          id = 'JetBrains'
          name = 'JetBrains Team'
          organization = 'JetBrains'
          organizationUrl = 'https://www.jetbrains.com'
        }
      }
      scm {
        connection = 'scm:git:git@github.com:JetBrains/intellij-coverage.git'
        developerConnection = 'scm:git:ssh:github.com/JetBrains/intellij-coverage.git'
        url = 'https://github.com/JetBrains/intellij-coverage'
      }
    }
  }
}
