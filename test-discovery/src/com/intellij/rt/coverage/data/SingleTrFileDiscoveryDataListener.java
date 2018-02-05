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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import static com.intellij.rt.coverage.data.TrFileDiscoveryDataListener.writeVisitedMethod;

public class SingleTrFileDiscoveryDataListener implements TestDiscoveryDataListener  {
  @SuppressWarnings("WeakerAccess")
  public static final String TRACE_FILE = "org.jetbrains.instrumentation.trace.file";
  @SuppressWarnings("WeakerAccess")
  public static final String BUFFER_SIZE = "org.jetbrains.instrumentation.trace.file.buffer.size";

  private final BufferedOutputStream bw;

  public SingleTrFileDiscoveryDataListener() throws Exception {
    String myTraceFile = System.getProperty(TRACE_FILE, "td.tr");
    bw = new BufferedOutputStream(new FileOutputStream(myTraceFile), Integer.parseInt(System.getProperty(BUFFER_SIZE, "32768")));
  }

  public void testFinished(String testName, Map<String, boolean[]> classToVisitedMethods, Map<String, String[]> classToMethodNames) throws IOException {
    writeVisitedMethod(classToVisitedMethods, classToMethodNames, new DataOutputStream(bw) {
      @Override
      public void close() {
      }
    });
  }

  public void testsFinished() {
    try {
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
