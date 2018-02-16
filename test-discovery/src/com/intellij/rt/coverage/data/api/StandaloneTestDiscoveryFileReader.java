/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package com.intellij.rt.coverage.data.api;

import java.io.File;
import java.io.IOException;

public class StandaloneTestDiscoveryFileReader {
  public static void main(String[] args) throws IOException {
    TestDiscoveryProtocolUtil.readFile(new File(args[0]), new SimpleDecodingTestDiscoveryProtocolReader() {
      @Override
      protected void processData(String testClassName, String testMethodName, String className, String methodName) {
        System.out.println(testClassName + "." + testMethodName + " uses " + className + "." + methodName);
      }

      public void processMetadataEntry(String key, String value) {
        System.out.println("Metadata entry: " + key + " = " + value);
      }
    });
  }
}
