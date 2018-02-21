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

import com.intellij.rt.coverage.data.TestDiscoveryProjectData;
import com.intellij.rt.coverage.instrumentation.AbstractIntellijClassfileTransformer;
import com.intellij.rt.coverage.testDiscovery.instrumentation.TestDiscoveryInstrumenter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.ClassWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExplicitTestDiscoveryInstrumentationTest {

  @Before
  public void setUp() {
    System.setProperty(TestDiscoveryProjectData.TEST_DISCOVERY_DATA_LISTENER_PROP, DeafTestDiscoveryDataListener.class.getName());
    System.setProperty("idea.inline.counter.fields", "true");
  }

  @After
  public void tearDown() {
    System.clearProperty(TestDiscoveryProjectData.TEST_DISCOVERY_DATA_LISTENER_PROP);
    System.clearProperty("idea.inline.counter.fields");
  }


  private byte[] doTransform(final String name) throws IOException {
    AbstractIntellijClassfileTransformer testDiscoveryInstrumenter = new AbstractIntellijClassfileTransformer() {
      protected ClassVisitor createClassVisitor(String className, ClassLoader loader, ClassReader cr, ClassWriter cw) {
        return new TestDiscoveryInstrumenter(cw, cr, className, loader);
      }

      protected boolean shouldExclude(String className) {
        return false;
      }
    };

    final String resource = name.replace('.', '/') + ".class";
    ClassLoader loader = MySerializable.class.getClassLoader();
    byte[] bytes = readBytes(loader.getResourceAsStream(resource));
    return testDiscoveryInstrumenter.instrument(bytes, name, loader, true);
  }

  public static class MySerializable implements Serializable {
    private final String field;

    public MySerializable(final String field) {
      this.field = field;
    }

    public String getField() {
      return field;
    }
  }

  @Test
  public void testSerializable() throws Throwable {
    String name = MySerializable.class.getName();
    Object transformed = 
        new TransformedClassLoader(MySerializable.class.getClassLoader(), name, doTransform(name))
        .loadClass(name, true)
        .getConstructor(String.class)
        .newInstance("hello");
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    new ObjectOutputStream(buffer).writeObject(transformed);
    Object restored = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray())).readObject();
    assertTrue(restored instanceof MySerializable);
    assertEquals("hello", ((MySerializable) restored).getField());
  }

  @SuppressWarnings("ALL")
  public static class InitClass {
    public static void initInit() {
      B b = new B(0); //leads to: A<clinit>; A(1); B(1); B<clinit>; A(0); B(0)
    }

    public static class A {
      static final A CONST = new B(1);
      transient int pp;

      public A(int p) {
        pp = p;
      }
    }

    public static class B extends A {
      public B(int i) {
        super(1);
      }
    }
  }

  @Test
  public void testBrokenInitializer() throws Throwable {
    String name = InitClass.B.class.getName();
    Object transformed = 
        new TransformedClassLoader(InitClass.B.class.getClassLoader(), name, doTransform(name))
        .loadClass(name, true)
        .getConstructor(int.class)
        .newInstance(1);
    //ensure class instrumentation doesn't fail
    assertNotNull(transformed);
  }

  private class TransformedClassLoader extends ClassLoader {
    private final String name;
    private final byte[] bytes;

    TransformedClassLoader(ClassLoader parent, String name, byte[] bytes) {
      super(parent);
      this.name = name;
      this.bytes = bytes;
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException {
      if (name.equals(this.name)) {
        Class<?> c = defineClass(name, bytes, 0, bytes.length);
        if (resolve) {
          resolveClass(c);
        }
        return c;
      }
      return super.loadClass(name, resolve);
    }
  }
  
  private static byte[] readBytes(@NotNull InputStream in) throws IOException{
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[16384];
    try {
      for (int len = in.read(buffer); len > 0 ; len = in.read(buffer)) {
        out.write(buffer, 0, len);
      }
      return out.toByteArray();
    } finally {
      in.close();
    }
  }

  
}
