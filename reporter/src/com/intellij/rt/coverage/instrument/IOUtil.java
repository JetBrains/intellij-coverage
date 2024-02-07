/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrument;

import com.intellij.rt.coverage.util.CoverageIOUtil;

import java.io.*;

public class IOUtil {
  public static byte[] readBytes(InputStream is) throws IOException {
    ByteArrayOutputStream os = null;
    try {
      final byte[] buffer = new byte[4096];
      os = new ByteArrayOutputStream();
      int read;
      while ((read = is.read(buffer)) != -1) {
        os.write(buffer, 0, read);
      }
    } finally {
      CoverageIOUtil.close(os);
    }
    return os.toByteArray();
  }

  public static byte[] readBytes(File file) throws IOException {
    InputStream is = null;
    try {
      is = new BufferedInputStream(new FileInputStream(file));
      return readBytes(is);
    } finally {
      CoverageIOUtil.close(is);
    }
  }

  public static void writeBytes(File file, byte[] bytes) throws IOException {
    OutputStream os = null;
    try {
      os = new BufferedOutputStream(new FileOutputStream(file));
      os.write(bytes);
    } finally {
      CoverageIOUtil.close(os);
    }
  }
}
