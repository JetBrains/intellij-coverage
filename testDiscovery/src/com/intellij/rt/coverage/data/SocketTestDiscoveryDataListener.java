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

import com.intellij.rt.coverage.util.CoverageIOUtil;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class SocketTestDiscoveryDataListener implements TestDiscoveryDataListener {
  public static final String HOST_PROP = "test.discovery.data.host";
  public static final String PORT_PROP = "test.discovery.data.port";

  private static final byte START_MSG = 0;
  private static final byte FINISHED_MSG = 1;
  private static final byte TEST_FINISHED_MSG = 2;
  private final SocketChannel mySocket;
  private final ExecutorService myExecutor;

  public SocketTestDiscoveryDataListener() throws IOException {
    String host = System.getProperty(HOST_PROP, "127.0.0.1");
    int port = Integer.parseInt(System.getProperty(PORT_PROP));
    mySocket = SocketChannel.open(new InetSocketAddress(host, port));
    mySocket.configureBlocking(false);
    myExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
      public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName("TestDiscoveryDataWriter");
        return thread;
      }
    });
    write(new WriteOp() {
      public void write() throws IOException {
        mySocket.write(ByteBuffer.wrap(new byte[]{START_MSG}));
      }
    });
  }

  public void testFinished(final String testName, Map<String, boolean[]> classToVisitedMethods, Map<String, String[]> classToMethodNames) {
    final List<VisitedMethods> visitedMethods = new ArrayList<VisitedMethods>();
    String[] methodNames = null;
    for (Map.Entry<String, boolean[]> e : classToVisitedMethods.entrySet()) {
      VisitedMethods currentMethods = null;
      boolean[] markers = e.getValue();
      for (int i = 0; i < markers.length; i++) {
        boolean marker = markers[i];
        if (marker) {
          if (currentMethods == null) {
            currentMethods = new VisitedMethods(e.getKey());
            methodNames = classToMethodNames.get(e.getKey());
            visitedMethods.add(currentMethods);
          }
          currentMethods.methodNames.add(methodNames[i]);
        }
      }
    }

    write(new WriteOp() {
      public void write() throws IOException {
        final MyByteArrayOutputStream baos = new MyByteArrayOutputStream();
        baos.write(TEST_FINISHED_MSG);
        DataOutputStream dos = new DataOutputStream(baos);
        CoverageIOUtil.writeUTF(dos, testName);

        CoverageIOUtil.writeINT(dos, visitedMethods.size());
        for (VisitedMethods ns : visitedMethods) {
          CoverageIOUtil.writeUTF(dos, ns.className);
          CoverageIOUtil.writeINT(dos, ns.methodNames.size());
          for (String name : ns.methodNames) {
            CoverageIOUtil.writeUTF(dos, name);
          }
        }

        mySocket.write(baos.asByteBuffer());
      }
    });
  }

  public void testsFinished() {
    write(new WriteOp() {
      public void write() throws IOException {
        mySocket.write(ByteBuffer.wrap(new byte[]{FINISHED_MSG}));
      }
    });
    myExecutor.shutdown();
    try {
      mySocket.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void write(final WriteOp op) {
    myExecutor.submit(new Runnable() {
      public void run() {
        try {
          op.write();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
  }

  private interface WriteOp {
    void write() throws IOException;
  }

  private static class MyByteArrayOutputStream extends ByteArrayOutputStream {
    ByteBuffer asByteBuffer() {
      return ByteBuffer.wrap(buf, 0, count);
    }
  }

  private static class VisitedMethods {
    private final String className;
    private final List<String> methodNames = new ArrayList<String>(1);

    private VisitedMethods(String className) {
      this.className = className;
    }
  }
}
