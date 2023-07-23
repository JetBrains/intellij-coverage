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

package com.intellij.rt.coverage.instrumentation.dataAccess;

import com.intellij.rt.coverage.instrumentation.CoverageRuntime;
import com.intellij.rt.coverage.instrumentation.InstrumentationUtils;
import com.intellij.rt.coverage.instrumentation.data.InstrumentationData;
import com.intellij.rt.coverage.instrumentation.data.Key;
import com.intellij.rt.coverage.util.OptionsUtil;

public class DataAccessUtil {
  public static final String HITS_ARRAY_TYPE = "[I";
  public static final String TEST_MASK_ARRAY_TYPE = "[Z";

  public static final String CLASS_DATA_NAME = "__$classData$__";


  public static CoverageDataAccess createTestTrackingDataAccess(InstrumentationData data, boolean isArray) {
    String className = data.get(Key.CLASS_NAME);
    if (OptionsUtil.FIELD_INSTRUMENTATION_ENABLED) {
      CoverageDataAccess.Init init = isArray ? createTestTrackingArrayInit(className) : createTestTrackingInit(className, false);
      return new FieldCoverageDataAccess(data.get(Key.CLASS_READER), className, init);
    } else {
      return new NameCoverageDataAccess(createTestTrackingInit(className, true));
    }
  }

  private static CoverageDataAccess.Init createTestTrackingInit(String className, boolean needCache) {
    return new CoverageDataAccess.Init(CLASS_DATA_NAME, InstrumentationUtils.OBJECT_TYPE, CoverageRuntime.COVERAGE_RUNTIME_OWNER,
        needCache ? "loadClassDataCached" : "loadClassData", "(Ljava/lang/String;)" + InstrumentationUtils.OBJECT_TYPE, new Object[]{className});
  }

  private static CoverageDataAccess.Init createTestTrackingArrayInit(String className) {
    return new CoverageDataAccess.Init("__$traceMask$__", TEST_MASK_ARRAY_TYPE, CoverageRuntime.COVERAGE_RUNTIME_OWNER,
        "getTraceMask", "(Ljava/lang/String;)" + TEST_MASK_ARRAY_TYPE, new Object[]{className});
  }
}
