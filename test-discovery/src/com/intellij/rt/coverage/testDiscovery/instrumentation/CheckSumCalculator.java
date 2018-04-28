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

package com.intellij.rt.coverage.testDiscovery.instrumentation;

import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.util.Printer;
import org.jetbrains.coverage.org.objectweb.asm.util.Textifier;
import org.jetbrains.coverage.org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class CheckSumCalculator extends ClassVisitor {
  private final Map<String, byte[]> checksums = new HashMap<String, byte[]>();
  private final MessageDigest messageDigest;
  private final String className;

  public CheckSumCalculator(int api, String className) {
    super(api, new TraceClassVisitor(null, new ChecksumPrinter(api), null));
    this.className = className;
    try {
      this.messageDigest = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public Map<String, byte[]> getChecksums() {
    return checksums;
  }

  private Printer getPrinter() {
    return ((TraceClassVisitor) cv).p;
  }

  private void saveChecksum(String name) {
    PrintWriter pw = null;
    try {
      StringWriter sw = new StringWriter();
      pw = new PrintWriter(sw);
      getPrinter().print(pw);
      checksums.put(name, messageDigest.digest(sw.toString().getBytes()));
    } finally {
      getPrinter().getText().clear();
      if (pw != null) pw.close();
    }
  }

  /**
   * visiting only relevant methods, see {@link InstrumentedMethodsCollector}
   */
  @Override
  public MethodVisitor visitMethod(int access, final String name,
                                   final String desc, String signature,
                                   String[] exceptions) {
    if (!checksums.containsKey(className)) {
      // class declaration checksum
      saveChecksum(className);
    }
    return new MethodVisitor(api, super.visitMethod(access, name, desc, signature, exceptions)) {
      @Override
      public void visitEnd() {
        super.visitEnd();
        // method checksum
        saveChecksum(TestDiscoveryInstrumentationUtils.getMethodId(name, desc));
      }
    };
  }

  // TODO: directly extend asm Printer instead of Textifier to avoid string building overhead
  private static class ChecksumPrinter extends Textifier {
    ChecksumPrinter(int api) {
      super(api);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
      // no-op
    }

    @Override
    protected Textifier createTextifier() {
      return new ChecksumPrinter(api);
    }
  }
}
