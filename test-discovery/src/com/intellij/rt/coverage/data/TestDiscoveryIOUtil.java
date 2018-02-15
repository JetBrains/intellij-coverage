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

import com.intellij.rt.coverage.util.CoverageIOUtil;

import java.io.DataInput;
import java.io.IOException;

public class TestDiscoveryIOUtil {
  public interface DictionaryProcessor {
    void process(int id, String name);
  }

  public static void readDictionary(DataInput input, DictionaryProcessor processor) throws IOException {
    int count = CoverageIOUtil.readINT(input);
    while (count-- > 0) {
      int id = CoverageIOUtil.readINT(input);
      String name = CoverageIOUtil.readUTFFast(input);
      processor.process(id, name);
    }
  }
}
