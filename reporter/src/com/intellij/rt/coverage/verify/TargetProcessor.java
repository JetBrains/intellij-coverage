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

package com.intellij.rt.coverage.verify;

import com.intellij.rt.coverage.data.ProjectData;

/**
 * This class is calculating coverage summary with the target granularity.
 */
public interface TargetProcessor {
  void process(ProjectData projectData, Consumer consumer);

  /**
   * The processor is passing the coverage result of a single element.
   * For example, class target processor is passing coverage summary for each class.
   */
  interface Consumer {
    /**
     * A callback with coverage calculated for single element with target type.
     *
     * @param name     element name, for example class name
     * @param coverage coverage summary for the element
     */
    void consume(String name, Verifier.CollectedCoverage coverage);
  }
}
