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

package com.intellij.rt.coverage;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TransformedClassLoader extends ClassLoader {
  private final String name;
  private final byte[] bytes;

  public TransformedClassLoader(ClassLoader parent, String name, byte[] bytes) {
    super(parent);
    this.name = name;
    this.bytes = bytes;
  }

  @Override
  public synchronized Class<?> loadClass(String name, boolean resolve)
      throws ClassNotFoundException {
    if (name.equals(this.name)) {
      Class<?> c = defineClass(name, bytes, 0, bytes.length);
      if (resolve) {
        resolveClass(c);
      }
      return c;
    }
    return super.loadClass(name, resolve);
  }

  public static byte[] readBytes(@NotNull InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[16384];
    try {
      for (int len = in.read(buffer); len > 0; len = in.read(buffer)) {
        out.write(buffer, 0, len);
      }
      return out.toByteArray();
    } finally {
      in.close();
    }
  }
}
