/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

package com.intellij.rt.coverage.jmh;

import junit.framework.TestResult;
import junit.textui.TestRunner;
import org.joda.time.TestAllPackages;

import java.util.Locale;
import java.util.TimeZone;

public class JodaTimeTests {
  private static void runTests(TestRunner runner) throws Exception {
    // setup a time zone other than one tester is in
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

    // setup a locale other than one the tester is in
    Locale.setDefault(new Locale("th", "TH"));

    runner.start(new String[]{
        TestAllPackages.class.getName()
    });
  }

  public static void testDiscovery() throws Exception {
    runTests(new TestRunner() {
      @Override
      protected TestResult createTestResult() {
        TestResult result = super.createTestResult();
        result.addListener(new TestDiscoveryListener());
        return result;
      }
    });
  }

  public static void testCoverage() throws Exception {
    runTests(new TestRunner());
  }
}
