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

import com.intellij.rt.coverage.data.SocketTestDataReader;
import com.intellij.rt.coverage.data.SocketTestDiscoveryDataListener;
import com.intellij.rt.coverage.data.TrProtocolTestDiscoveryDataListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.DataInputStream;
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
    ops.add("-Dtest.discovery.data.listener=" + SocketTestDiscoveryDataListener.class.getName());
    ops.add("-D" + SocketTestDiscoveryDataListener.HOST_PROP + "=127.0.0.1");
    ops.add("-D" + SocketTestDiscoveryDataListener.PORT_PROP + "=" + socketListener.getPort());
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
            MyTestDataListener listener = new MyTestDataListener();

            while (socket.isConnected()) {
              int code = inputStream.read();
              if (code == -1) return;
              switch (code) {
                case TrProtocolTestDiscoveryDataListener.START_MARKER:
                  // do nothing
                  Assert.assertEquals(SocketTestDiscoveryDataListener.VERSION, inputStream.read());
                  break;
                case TrProtocolTestDiscoveryDataListener.FINISH_MARKER:
                  socket.close();
                  finished = true;
                  return;
                case TrProtocolTestDiscoveryDataListener.NAMES_DICTIONARY_PART_MARKER:
                  SocketTestDataReader.readDictionary(new DataInputStream(inputStream), listener);
                  break;
                case TrProtocolTestDiscoveryDataListener.TEST_FINISHED_MARKER:
                  SocketTestDataReader.readTestData(new DataInputStream(inputStream), listener);
                  break;
              }
            }
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

    private class MyTestDataListener extends SocketTestDataReader {
      private String testName;

      @Override
      protected void processTestName(int testClassId, int testMethodId) {
        testName = enumerator.get(testClassId) + "." + enumerator.get(testMethodId);
      }

      @Override
      protected void processUsedMethod(int classId, int methodId) {
        testDiscoveryData.add(new String[] {testName, enumerator.get(classId), enumerator.get(methodId)});
      }

      @Override
      protected void processEnumeratedName(int id, String name) {
        enumerator.put(id, name);
      }
    }
  }
}
