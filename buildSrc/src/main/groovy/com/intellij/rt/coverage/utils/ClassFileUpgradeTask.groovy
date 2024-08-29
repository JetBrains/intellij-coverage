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

package com.intellij.rt.coverage.utils

import groovy.transform.CompileStatic
import org.apache.commons.io.IOUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

@CompileStatic
abstract class ClassFileUpgradeTask extends DefaultTask {
  @InputFiles
  FileCollection inputFiles

  @OutputDirectory
  Directory outputDirectory

  @TaskAction
  void transform() {
    def inputJars = inputFiles.files.findAll { it.name.endsWith(".jar") }
    List<ZipFile> zipFiles = []
    try {
      inputJars.each {
        zipFiles << new ZipFile(it)
      }
      process(zipFiles)
    } finally {
      zipFiles.each { it.close() }
    }
  }

  private void process(List<ZipFile> zipFiles) {
    def superClasses = collectSuperClasses(zipFiles)
    transformJars(zipFiles, superClasses)
  }

  private Map<String, String> collectSuperClasses(List<ZipFile> zipFiles) {
    Map<String, String> superClasses = [:]
    zipFiles.each { zip ->
      eachEntry(zip) { ZipEntry classEntry ->
        if (!classEntry.name.endsWith(".class")) {
          return
        }
        zip.getInputStream(classEntry).withStream { inputStream ->
          def reader = new ClassReader(inputStream)
          superClasses[reader.className] = reader.superName
        }
      }
    }
    return superClasses
  }

  private void transformJars(List<ZipFile> zipFiles, Map<String, String> superClasses) {
    zipFiles.each { zip ->
      def outputFile = outputDirectory.file(new File(zip.name).name).asFile
      new ZipOutputStream(outputFile.newOutputStream()).withStream { output ->
        eachEntry(zip) { ZipEntry entry ->
          output.putNextEntry(new ZipEntry(entry.name))
          if (!entry.isDirectory()) {
            zip.getInputStream(entry).withStream { input ->
              if (entry.name.endsWith(".class")) {
                def bytes = transformClass(input, superClasses)
                IOUtils.write(bytes, output)
              } else {
                IOUtils.copy(input, output)
              }
            }
          }
          output.closeEntry()
        }
      }
    }
  }

  private byte[] transformClass(InputStream stream, Map<String, String> superClasses) {
    def reader = new ClassReader(stream)
    def writer = new HierarchyClassWriter(superClasses)
    reader.accept(new UpgradingClassVisitor(writer), 0)
    return writer.toByteArray()
  }

  private void eachEntry(ZipFile zip, Closure<?> callback) {
    zip.entries().iterator().each { ZipEntry entry ->
      callback(entry)
    }
  }
}
