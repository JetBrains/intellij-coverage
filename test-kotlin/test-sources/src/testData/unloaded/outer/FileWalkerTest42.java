/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

package testData.unloaded.outer;


import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

class FileWalkerTest42 {                   // coverage: NONE
  static void walker(Path path) throws IOException {
    final Runnable x = () -> {             // coverage: NONE
      System.out.println(42);              // coverage: NONE
    }; // coverage: NONE
    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {  // coverage: NONE
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        return FileVisitResult.CONTINUE;                      // coverage: NONE
      }
    });
  } // coverage: NONE
}
