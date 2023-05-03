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

package com.intellij.rt.coverage.aggregate.api;

import com.intellij.rt.coverage.report.api.Filters;
import com.intellij.rt.coverage.util.classFinder.ClassFilter;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A request to collect all classes that match filter to a specified binary report file.
 */
public class Request {
  public final ClassFilter.PatternFilter classFilter;
  public final List<Pattern> excludeAnnotations;
  public final File outputFile;
  public final File smapFile;

  public Request(Filters filters, File outputFile, File smapFile) {
    this.classFilter = new ClassFilter.PatternFilter(filters.includeClasses, filters.excludeClasses);
    this.excludeAnnotations = filters.excludeAnnotations;
    this.outputFile = outputFile;
    this.smapFile = smapFile;
  }
}
