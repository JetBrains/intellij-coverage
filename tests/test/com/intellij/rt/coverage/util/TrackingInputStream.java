/*
 * Copyright 2000-2026 JetBrains s.r.o.
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

package com.intellij.rt.coverage.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

class TrackingInputStream extends ByteArrayInputStream {
  private boolean myClosed;

  TrackingInputStream(byte[] bytes) {
    super(bytes);
  }

  public void close() throws IOException {
    myClosed = true;
    super.close();
  }

  boolean isClosed() {
    return myClosed;
  }
}
