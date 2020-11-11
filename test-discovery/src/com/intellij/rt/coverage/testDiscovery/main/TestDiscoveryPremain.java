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

package com.intellij.rt.coverage.testDiscovery.main;

import com.intellij.rt.coverage.data.TestDiscoveryProjectData;
import com.intellij.rt.coverage.testDiscovery.instrumentation.OpenCloseFileTransformer;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class TestDiscoveryPremain {
  public static final String INCLUDE_PATTERNS_VM_OP = "test.discovery.include.class.patterns";
  @SuppressWarnings("WeakerAccess")
  public static final String EXCLUDE_PATTERNS_VM_OP = "test.discovery.exclude.class.patterns";

  private void performPremain(Instrumentation instrumentation) throws Exception {
    System.out.println("---- IntelliJ IDEA Test Discovery ---- ");

    // separated by ;
    final List<Pattern> include = patterns(INCLUDE_PATTERNS_VM_OP);
    final List<Pattern> exclude = patterns(EXCLUDE_PATTERNS_VM_OP);

    // initialize before instrumentation
    @SuppressWarnings("unused")
    TestDiscoveryProjectData projectData = TestDiscoveryProjectData.getProjectData();

    instrumentation.addTransformer(new TestDiscoveryTransformer(exclude, include));

    addOpenCloseTransformer(instrumentation);
  }

  private static void addOpenCloseTransformer(Instrumentation instrumentation) throws UnmodifiableClassException {
    if (!Boolean.parseBoolean(System.getProperty(TestDiscoveryProjectData.TRACK_FILES, "true"))) {
      System.out.println("Tracking for opened/closed files disabled by '" + TestDiscoveryProjectData.TRACK_FILES + "' system property");
      return;
    }

    if (System.getProperty(TestDiscoveryProjectData.AFFECTED_ROOTS) == null) {
      System.out.println("Tracking for opened/closed files disabled due to undefined '" + TestDiscoveryProjectData.AFFECTED_ROOTS + "' system property");
      return;
    }

    OpenCloseFileTransformer openCloseFileTransformer = new OpenCloseFileTransformer();
    instrumentation.addTransformer(openCloseFileTransformer, true);
    instrumentation.retransformClasses(openCloseFileTransformer.classesToTransform());
  }

  private static List<Pattern> patterns(String key) {
    String property = System.getProperty(key);
    if (property == null) return Collections.emptyList();
    System.out.println("Patterns from " + key);
    List<Pattern> patterns = new ArrayList<Pattern>(1);
    for (String s : property.split(";")) {
      try {
        patterns.add(Pattern.compile(s));
        System.out.println(s);
      } catch (PatternSyntaxException ex) {
        System.err.println("Problem occurred with pattern " + s);
        System.err.println(ex.getDescription());
        System.exit(1);
      }
    }
    return patterns;
  }

  public static void premain(String argsString, Instrumentation instrumentation) throws Exception {
    new TestDiscoveryPremain().performPremain(instrumentation);
  }
}
