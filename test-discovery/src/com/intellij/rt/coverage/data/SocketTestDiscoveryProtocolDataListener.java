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

package com.intellij.rt.coverage.data;

import org.jetbrains.coverage.gnu.trove.TIntArrayList;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@SuppressWarnings("unused")
public class SocketTestDiscoveryProtocolDataListener extends TestDiscoveryProtocolDataListener {
  private static final int SOCKET_BUFFER_SIZE = 128 * 1024;
  @SuppressWarnings("WeakerAccess")
  public static final String HOST_PROP = "test.discovery.data.host";
  @SuppressWarnings("WeakerAccess")
  public static final String PORT_PROP = "test.discovery.data.port";
  @SuppressWarnings("WeakerAccess")
  public static final String DATA_VERSION = "test.discovery.data.version";

  private static final byte DEFAULT_VERSION = 1;

  private final Socket mySocket;
  private final BlockingQueue<ByteBuffer> myData = new ArrayBlockingQueue<ByteBuffer>(10);
  private final NameEnumerator.Incremental incrementalNameEnumerator = new NameEnumerator.Incremental();
  private final DataOutputStream dos;
  private final OutputStream os;

  public SocketTestDiscoveryProtocolDataListener() throws IOException {
    super(Byte.parseByte(System.getProperty(DATA_VERSION, String.valueOf(DEFAULT_VERSION))));
    String host = System.getProperty(HOST_PROP, "127.0.0.1");
    int port = Integer.parseInt(System.getProperty(PORT_PROP));
    mySocket = new Socket(host, port);
    mySocket.setSendBufferSize(SOCKET_BUFFER_SIZE);
    mySocket.setTcpNoDelay(true);
    os = mySocket.getOutputStream();
    dos = new DataOutputStream(os);
    start(dos);
  }

  public void testFinished(String className, String methodName, Map<Integer, boolean[]> classToVisitedMethods, Map<Integer, int[]> classToMethodNames, List<int[]> openedFiles) {
    try {
      writeTestFinished(dos, className, methodName, classToVisitedMethods, classToMethodNames, openedFiles);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void testsFinished() {
    try {
      finish(dos);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        dos.close();
        os.close();
        mySocket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void addMetadata(Map<String, String> metadata) throws IOException {
    writeMetadata(dos, metadata);
  }

  public void addClassMetadata(List<ClassMetadata> metadata) throws IOException {
    writeClassMetadata(dos, metadata);
  }

  public NameEnumerator.Incremental getNameEnumerator() {
    return incrementalNameEnumerator;
  }

  private static class VisitedMethods {
    private final int classId;
    private final TIntArrayList methodIds = new TIntArrayList(1);

    private VisitedMethods(int classId) {
      this.classId = classId;
    }
  }
}
