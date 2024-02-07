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

package com.intellij.rt.coverage.instrument.api;

import com.intellij.rt.coverage.instrument.IOUtil;
import com.intellij.rt.coverage.instrument.Instrumentator;
import com.intellij.rt.coverage.report.api.Filters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class OfflineInstrumentationApi {
  private OfflineInstrumentationApi() {
    // no-op
  }

  public static void instrument(List<File> roots, List<File> outputRoots, Filters filters, boolean countHits) {
    new Instrumentator(roots, outputRoots, filters).instrument(countHits);
  }

  public static byte[] instrument(InputStream input, boolean countHits) throws IOException {
    byte[] bytes = IOUtil.readBytes(input);
    return Instrumentator.instrument(bytes, countHits);
  }
}
