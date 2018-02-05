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

package com.intellij.rt.coverage.data;

import java.io.*;

public class LongDataOutputStream extends OutputStream implements DataOutput {
  private long total;
  private DataOutputStream origin;

  public LongDataOutputStream(OutputStream out) {
    origin = new DataOutputStream(out);
  }

  private void inc(int value) {
    long temp = total + value;
    if (temp < 0) {
      temp = Long.MAX_VALUE;
    }
    total = temp;
  }

  public void write(int b) throws IOException {
    origin.write(b);
    inc(1);
  }

  public void write(byte b[], int off, int len) throws IOException {
    origin.write(b, off, len);
    inc(len);
  }

  public void flush() throws IOException {
    origin.flush();
  }

  public void writeBoolean(boolean v) throws IOException {
    origin.writeBoolean(v);
    inc(1);
  }

  public void writeByte(int v) throws IOException {
    origin.write(v);
    inc(1);
  }

  public void writeShort(int v) throws IOException {
    origin.writeShort(v);
    inc(2);
  }

  public void writeChar(int v) throws IOException {
    origin.writeChar(v);
    inc(2);
  }

  public void writeInt(int v) throws IOException {
    origin.writeInt(v);
    inc(4);
  }

  public void writeLong(long v) throws IOException {
    origin.writeLong(v);
    inc(8);
  }

  public void writeFloat(float v) throws IOException {
    origin.writeFloat(v);
  }

  public void writeDouble(double v) throws IOException {
    origin.writeDouble(v);
  }

  public void writeBytes(String s) throws IOException {
    origin.writeBytes(s);
    inc(s.length());
  }

  public void writeChars(String s) throws IOException {
    origin.writeChars(s);
    inc(s.length() * 2);
  }

  public void writeUTF(String str) throws IOException {
    origin.writeUTF(str);
  }

  @Override
  public void close() throws IOException {
    origin.close();
  }

  public long total() {
    return total;
  }
}
