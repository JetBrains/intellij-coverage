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

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.InstrumentationUtils;
import com.intellij.rt.coverage.util.OptionsUtil;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

public class DataAccessUtil {
  public static final String HITS_ARRAY_TYPE = "[I";
  public static final String TEST_MASK_ARRAY_TYPE = "[Z";


  private static final CoverageDataAccess.DataType HITS_TYPE = new CoverageDataAccess.DataType("__$hits$__", HITS_ARRAY_TYPE,
      ProjectData.PROJECT_DATA_OWNER, "getHitsMask", "(Ljava/lang/String;)" + HITS_ARRAY_TYPE);
  private static final CoverageDataAccess.DataType CONDY_HITS_TYPE = new CoverageDataAccess.DataType("__$hits$__", HITS_ARRAY_TYPE,
      "com/intellij/rt/coverage/util/CondyUtils", "getHitsMask", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;)" + HITS_ARRAY_TYPE);

  public static CoverageDataAccess createDataAccess(String className, ClassReader cr, boolean isSampling) {
    if (isSampling && OptionsUtil.NEW_SAMPLING_ENABLED || !isSampling && OptionsUtil.NEW_TRACING_ENABLED) {
      if (OptionsUtil.CONDY_ENABLED && InstrumentationUtils.getBytecodeVersion(cr) >= Opcodes.V11) {
        return new CondyCoverageDataAccess(className, CONDY_HITS_TYPE);
      } else {
        return new FieldCoverageDataAccess(cr, className, HITS_TYPE);
      }
    } else {
      return new NameCoverageDataAccess(className, HITS_TYPE);
    }
  }

  public static String CLASS_DATA_NAME = "__$classData$__";
  private static final CoverageDataAccess.DataType TEST_TRACKING_TYPE = new CoverageDataAccess.DataType(CLASS_DATA_NAME, InstrumentationUtils.OBJECT_TYPE,
      ProjectData.PROJECT_DATA_OWNER, "loadClassData", "(Ljava/lang/String;)" + InstrumentationUtils.OBJECT_TYPE);

  private static final CoverageDataAccess.DataType TEST_TRACKING_ARRAY_TYPE = new CoverageDataAccess.DataType("__$traceMask$__", TEST_MASK_ARRAY_TYPE,
      ProjectData.PROJECT_DATA_OWNER, "getTraceMask", "(Ljava/lang/String;)" + TEST_MASK_ARRAY_TYPE);


  public static CoverageDataAccess createTestTrackingDataAccess(String className, ClassReader cr, boolean isArray) {
    if (OptionsUtil.NEW_TRACING_ENABLED) {
      return new FieldCoverageDataAccess(cr, className, isArray ? TEST_TRACKING_ARRAY_TYPE : TEST_TRACKING_TYPE);
    } else {
      return new NameCoverageDataAccess(className, TEST_TRACKING_TYPE);
    }
  }
}
