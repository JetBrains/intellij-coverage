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

package com.intellij.rt.coverage.testDiscovery;

import com.intellij.rt.coverage.data.SocketTestDiscoveryProtocolDataListener;
import com.intellij.rt.coverage.data.api.TestDiscoveryProtocolReader;
import com.intellij.rt.coverage.data.api.TestDiscoveryProtocolUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SocketWriterTestDiscoveryIntegrationTest {
  @Rule
  public TemporaryFolder tmpDir  = new TemporaryFolder();

  @Test
  public void testSimple() throws Exception {
    final List<String[]> result = doTest("simple");
    assertThat(result).isNotEmpty();
    assertThat(result).contains(
        new String[]{"Test.test1", "Test", "test1"},
        new String[]{"Test.test1", "ClassA", "method1"},
        new String[]{"Test.test1", "ClassA", "method2"},

        new String[]{"Test.test2", "Test", "test2"},
        new String[]{"Test.test2", "ClassB", "method1"},
        new String[]{"Test.test2", "ClassB", "method2"},

        new String[]{"Test.test3", "Test", "test3"},
        new String[]{"Test.test3", "ClassA", "methodR"},
        new String[]{"Test.test3", "ClassA", "method1"},
        new String[]{"Test.test3", "ClassA", "method2"},
        new String[]{"Test.test3", "ClassB", "methodR"},
        new String[]{"Test.test3", "ClassB", "method1"},
        new String[]{"Test.test3", "ClassB", "method2"}
    );
  }

  private List<String[]> doTest(final String directory) throws Exception {
    final File testData = getTestData(directory);
    final File outputDir = tmpDir.newFolder();

    TestDiscoveryTestUtil.compileTestData(testData, outputDir);

    MyTestDiscoverySocketListener socketListener = new MyTestDiscoverySocketListener();

    List<String> ops = new ArrayList<String>();
    ops.add("-Dtest.discovery.data.listener=" + SocketTestDiscoveryProtocolDataListener.class.getName());
    ops.add("-D" + SocketTestDiscoveryProtocolDataListener.HOST_PROP + "=127.0.0.1");
    ops.add("-D" + SocketTestDiscoveryProtocolDataListener.PORT_PROP + "=" + socketListener.getPort());
//     ops.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5007");

    TestDiscoveryTestUtil.runTestDiscovery(outputDir.getAbsolutePath(), "Test", ops);
    socketListener.waitForFinish();

    return socketListener.testDiscoveryData;
  }

  @NotNull
  private static File getTestData(@NotNull String directory) {
    final File file = new File(new File("").getAbsoluteFile(), "testData/testDiscovery/" + directory);
    assertThat(file).exists().isDirectory();
    return file;
  }

  private static class MyTestDiscoverySocketListener {
    private final ServerSocket socket = new ServerSocket(0);
    private final Thread socketThread;
    private final TIntObjectHashMap<String> enumerator = new TIntObjectHashMap<String>();
    private final List<String[]> testDiscoveryData = new ArrayList<String[]>();
    private volatile boolean finished;

    private MyTestDiscoverySocketListener() throws IOException {
      socketThread = new Thread(new Runnable() {
        public void run() {
          try {
            Socket socket = MyTestDiscoverySocketListener.this.socket.accept();
            InputStream inputStream = socket.getInputStream();
            TestDiscoveryProtocolUtil.readSequentially(inputStream, new MyTestDataListener());
            socket.close();
          } catch (Exception e) {
            Assert.fail(e.getMessage());
          }
        }
      }, "test-discovery-socket-listener");
      socketThread.start();

    }

    void waitForFinish() throws InterruptedException {
      int attempt = 0;
      while (attempt++ < 10) {
        Thread.sleep(1000);
        if (finished) {
          return;
        }
      }
      throw new RuntimeException("Socket server didn't receive test discovery finish message");
    }

    int getPort() {
      return socket.getLocalPort();
    }

    private class MyTestDataListener implements TestDiscoveryProtocolReader {
      public void testDiscoveryDataProcessingStarted(int version) {

      }

      public void testDiscoveryDataProcessingFinished() {
        finished = true;
      }

      public MetadataReader createMetadataReader() {
        return null;
      }

      public NameEnumeratorReader createNameEnumeratorReader() {
        return new NameEnumeratorReader() {
          public void enumerate(String name, int id) {
            enumerator.put(id, name);
          }
        };
      }

      public TestDataReader createTestDataReader(final int testClassId, int testMethodId) {
        final String testName = enumerator.get(testClassId) + "." + enumerator.get(testMethodId);
        return new TestDataReader() {
          int currentClassId;

          public void classProcessingStarted(int classId) {
            currentClassId = classId;
          }

          public void processUsedMethod(int methodId) {
            testDiscoveryData.add(new String[] {testName, enumerator.get(currentClassId), enumerator.get(methodId)});
          }

          public void classProcessingFinished(int classId) {

          }

          public void testDataProcessed() {

          }
        };
      }

      public void debug(String message) {

      }

      public void error(String message) {

      }

      public void error(Exception error) {

      }
    }
  }
}
