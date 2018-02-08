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

package org.junit.runner;

import org.junit.internal.RealSystem;
import org.junit.runner.notification.RunListener;

import java.lang.reflect.Method;

public class JUnitLauncher extends JUnitCore {

  public static void main(String... args) {
    final JUnitLauncher core = new JUnitLauncher();
    final TestDiscoveryJUnitRunListener listener = new TestDiscoveryJUnitRunListener();
    core.addListener(listener);
    final Integer[] retCode = new Integer[1];
    final Result result;
    try {
      result = core.runMain(new RealSystem() {
        @Override
        public void exit(int code) {
          retCode[0] = code;
        }
      }, args);
    } finally {
      listener.finish();
    }
    if (retCode[0] != null) {
      System.exit(retCode[0]);
    }
    System.exit(result.wasSuccessful() ? 0 : 1);
  }

  private static class TestDiscoveryJUnitRunListener extends RunListener {
    @Override
    public void testStarted(Description description) {

      final String className = description.getClassName();
      final String methodName = description.getMethodName();
      try {
        Object data = getData();
        Method testStarted = data.getClass().getMethod("testDiscoveryStarted", String.class, String.class);
        testStarted.invoke(data, className, methodName);
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }

    @Override
    public void testFinished(Description description) {
      final String className = description.getClassName();
      final String methodName = description.getMethodName();

      try {
        Object data = getData();
        Method testEnded = data.getClass().getMethod("testDiscoveryEnded", String.class, String.class);
        testEnded.invoke(data, className, methodName);
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }

    void finish() {
      try {
        Object data = getData();
        Method testEnded = data.getClass().getMethod("testDiscoveryFinished");
        testEnded.invoke(data);
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }

    private Object getData() throws Exception {
      return Class.forName("com.intellij.rt.coverage.data.TestDiscoveryProjectData")
          .getMethod("getProjectData")
          .invoke(null);
    }
  }
}