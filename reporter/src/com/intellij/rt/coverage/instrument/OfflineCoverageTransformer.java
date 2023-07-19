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

package com.intellij.rt.coverage.instrument;

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.CoverageTransformer;
import com.intellij.rt.coverage.instrumentation.dataAccess.CoverageDataAccess;
import com.intellij.rt.coverage.instrumentation.dataAccess.DataAccessUtil;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;

import java.util.List;
import java.util.regex.Pattern;

/**
 * This transformer instruments classes in such a way that these classes
 * can be initialized without information in ProjectData.
 * Namely, the bytecode generated with this transformer includes length of data array for coverage hits storage.
 * <p>
 * Offline instrumentation is required when there is no ability to install transformer on VM start.
 * Instead, offline transformation is run before application start with modification of class files on disk.
 *
 * @see com.intellij.rt.coverage.offline.RawProjectInit
 */
public class OfflineCoverageTransformer extends CoverageTransformer {
  public OfflineCoverageTransformer(ProjectData data, boolean shouldSaveSource, List<Pattern> excludePatterns, List<Pattern> includePatterns) {
    super(data, shouldSaveSource, excludePatterns, includePatterns);
  }

  @Override
  protected CoverageDataAccess.Init createInit(String className, ClassReader cr) {
    final int length = getRequiredArrayLength(cr);
    return new CoverageDataAccess.Init("__$hits$__", DataAccessUtil.HITS_ARRAY_TYPE, "com/intellij/rt/coverage/offline/RawProjectInit",
        "getOrCreateHitsMask", "(Ljava/lang/String;I)" + DataAccessUtil.HITS_ARRAY_TYPE, new Object[]{className, length});
  }

  @Override
  protected CoverageDataAccess.Init createCondyInit(String className, ClassReader cr) {
    final int length = getRequiredArrayLength(cr);
    return new CoverageDataAccess.Init("__$hits$__", DataAccessUtil.HITS_ARRAY_TYPE, "com/intellij/rt/coverage/offline/CondyUtils",
        "getOrCreateHitsMask", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;Ljava/lang/String;I)" + DataAccessUtil.HITS_ARRAY_TYPE, new Object[]{className, length});
  }

  private static int getRequiredArrayLength(ClassReader cr) {
    return analyseClassLength(cr).getHits();
  }

  private static ClassLengthAnalyser analyseClassLength(ClassReader cr) {
    final ClassLengthAnalyser analyser = new ClassLengthAnalyser();
    cr.accept(analyser, ClassReader.SKIP_FRAMES);
    return analyser;
  }
}
