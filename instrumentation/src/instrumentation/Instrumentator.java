/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.testDiscovery.TestDiscoveryInstrumenter;
import com.intellij.rt.coverage.util.ClassNameUtil;
import com.intellij.rt.coverage.util.ErrorReporter;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.ClassWriter;
import org.jetbrains.org.objectweb.asm.Opcodes;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class Instrumentator {

  public static void premain(String argsString, Instrumentation instrumentation) throws Exception {
    String[] args;
    if (argsString != null) {
      File argsFile = new File(argsString);
      if (argsFile.isFile()) {
        try {
          args = readArgsFromFile(argsString);
        } catch (IOException e) {
          ErrorReporter.reportError("Arguments were not passed correctly", e);
          return;
        }
      } else {
        args = tokenize(argsString);
      }
    }
    else {
      args = new String[0];
    }

    final boolean traceLines = args.length > 0 && Boolean.valueOf(args[1]).booleanValue();
    final boolean sampling = args.length == 0 || Boolean.valueOf(args[4]).booleanValue();
    final File dataFile = args.length > 0 ? new File(args[0]) : null;
    if (dataFile != null) {
      ErrorReporter.setBasePath(dataFile.getParent());
    }
    final boolean calcUnloaded = args.length > 0 && Boolean.valueOf(args[2]).booleanValue();
    ProjectData initialData = null;
    if (args.length > 0 && Boolean.valueOf(args[3]).booleanValue() && dataFile.isFile()) {
      initialData = ProjectDataLoader.load(dataFile);
    }
    int i = 5;

    final File sourceMapFile;
    if (args.length > 5 && Boolean.valueOf(args[5]).booleanValue()) {
      sourceMapFile = new File(args[6]);
      i = 7;
    } else {
      sourceMapFile = null;
    }

    final ProjectData data = args.length == 0 
            ? ProjectData.createProjectData() 
            : ProjectData.createProjectData(dataFile, initialData, traceLines, sampling);
    final List includePatterns;
    final List excludePatterns;
    if (!data.isTestDiscovery()) {
      includePatterns = new ArrayList();
      System.out.println("---- IntelliJ IDEA coverage runner ---- ");
      System.out.println(sampling ? "sampling ..." : ("tracing " + (traceLines ? "and tracking per test coverage ..." : "...")));
      final String excludes = "-exclude";
      System.out.println("include patterns:");
      for (; i < args.length; i++) {
        if (excludes.equals(args[i])) break;
        try {
          includePatterns.add(Pattern.compile(args[i]));
          System.out.println(args[i]);
        } catch (PatternSyntaxException ex) {
          System.err.println("Problem occurred with include pattern " + args[i]);
          System.err.println(ex.getDescription());
          System.err.println("This may cause no tests run and no coverage collected");
          System.exit(1);
        }
      }
      System.out.println("exclude patterns:");
      i++;
      excludePatterns = new ArrayList();
      for (; i < args.length; i++) {
        try {
          final Pattern pattern = Pattern.compile(args[i]);
          excludePatterns.add(pattern);
          System.out.println(pattern.pattern());
        } catch (PatternSyntaxException ex) {
          System.err.println("Problem occurred with exclude pattern " + args[i]);
          System.err.println(ex.getDescription());
          System.err.println("This may cause no tests run and no coverage collected");
          System.exit(1);
        }
  
      }
    }
    else {
      includePatterns = Collections.emptyList();
      excludePatterns = Collections.emptyList();
    }

    final ClassFinder cf = new ClassFinder(includePatterns, excludePatterns);
    if (dataFile != null) {
      final SaveHook hook = new SaveHook(dataFile, calcUnloaded, cf);
      hook.setSourceMapFile(sourceMapFile);
      Runtime.getRuntime().addShutdownHook(new Thread(hook));
    }

    instrumentation.addTransformer(new ClassFileTransformer() {
      private boolean computeFrames = computeFrames();

      public byte[] transform(ClassLoader loader,
                              String className,
                              Class classBeingRedefined,
                              ProtectionDomain protectionDomain,
                              byte[] classfileBuffer) throws IllegalClassFormatException {
        if (data.isStopped()) return null;
        try {
          if (className == null) {
            return null;
          }
          if (className.endsWith(".class")) {
            className = className.substring(0, className.length() - 6);
          }
          className = className.replace('\\', '.').replace('/', '.');

          //do not instrument itself
          //and do not instrument packages which are used during instrumented method invocation
          //(inside methods touch, save, etc from ProjectData)
          if (className.startsWith("com.intellij.rt.")
            || className.startsWith("java.")
            || className.startsWith("sun.")
            || className.startsWith("gnu.trove.")
            || className.startsWith("org.jetbrains.org.objectweb.asm.")
            || className.startsWith("org.apache.oro.text.regex.")) {
            return null;
          }

          // matching outer or inner class name depending on pattern
          if (ClassNameUtil.shouldExclude(className, excludePatterns)) return null;

          cf.addClassLoader(loader);
          if (includePatterns.isEmpty() && loader != null) {
            return instrument(classfileBuffer, data, className, loader, computeFrames, sourceMapFile != null);
          }
          for (Iterator it = includePatterns.iterator(); it.hasNext(); ) {
            if (((Pattern) it.next()).matcher(className).matches()) { // matching inner class name
              return instrument(classfileBuffer, data, className, loader, computeFrames, sourceMapFile != null);
            }
          }
        } catch (Throwable e) {
          ErrorReporter.reportError("Error during class instrumentation: " + className, e);
        }
        return null;
      }

      private boolean computeFrames() {
        return System.getProperty("idea.coverage.no.frames") == null;
      }
    });
  }

  private static String[] readArgsFromFile(String arg) throws IOException {
    final List result = new ArrayList();
    final File file = new File(arg);
    final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
    try {
      while (reader.ready()) {
        result.add(reader.readLine());
      }
    } finally {
      reader.close();
    }
    return (String[]) result.toArray(new String[result.size()]);
  }

  private static byte[] instrument(final byte[] classfileBuffer, final ProjectData data, String className, ClassLoader loader, boolean computeFrames, boolean shouldCalculateSource) {
    final ClassReader cr = new ClassReader(classfileBuffer);
    final ClassWriter cw;
    if (computeFrames) {
      final int version = getClassFileVersion(cr);
      cw = getClassWriter(version >= Opcodes.V1_6 && version != Opcodes.V1_1 ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS, loader);
    } else {
      cw = getClassWriter(ClassWriter.COMPUTE_MAXS, loader);
    }
    
    final ClassVisitor cv;
    if (data.isSampling()) {
      if (System.getProperty(ProjectData.TRACE_DIR) != null) {
        cv = new TestDiscoveryInstrumenter(cw, cr, className, loader);
      }
      else if (System.getProperty("idea.new.sampling.coverage") != null) {
        //wrap cw with new TraceClassVisitor(cw, new PrintWriter(new StringWriter())) to get readable bytecode  
        cv = new NewSamplingInstrumenter(data, cw, cr, className, shouldCalculateSource); 
      }
      else {
        cv = ((ClassVisitor) new SamplingInstrumenter(data, cw, className, shouldCalculateSource));
      }
    }
    else {
      cv = new ClassInstrumenter(data, cw, className, shouldCalculateSource);
    }
    cr.accept(cv, 0);
    return cw.toByteArray();
  }

  private static ClassWriter getClassWriter(int flags, final ClassLoader classLoader) {
    return new MyClassWriter(flags, classLoader);
  }

  public static int getClassFileVersion(ClassReader reader) {
    final int[] classFileVersion = new int[1];
    reader.accept(new ClassVisitor(Opcodes.ASM5) {
      public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        classFileVersion[0] = version;
      }
    }, 0);
    return classFileVersion[0];
  }

  private static String[] tokenize(String argumentString) {
    List tokenizedArgs = new ArrayList();
    StringBuffer currentArg = new StringBuffer();
    for (int i = 0; i < argumentString.length(); i++) {
      char c = argumentString.charAt(i);
      switch (c) {
        default:
          currentArg.append(c);
          break;
        case ' ':
          String arg = currentArg.toString();
          if (arg.length() > 0) {
            tokenizedArgs.add(arg);
          }
          currentArg = new StringBuffer();
          break;
        case '\"':
          for (i++; i < argumentString.length(); i++) {
            char d = argumentString.charAt(i);
            if (d == '\"') {
              break;
            }
            currentArg.append(d);
          }
      }
    }

    String arg = currentArg.toString();
    if (arg.length() > 0) {
      tokenizedArgs.add(arg);
    }
    return (String[])tokenizedArgs.toArray(new String[tokenizedArgs.size()]);
  }

  private static class MyClassWriter extends ClassWriter {
    public static final String JAVA_LANG_OBJECT = "java/lang/Object";
    private final ClassLoader classLoader;

    public MyClassWriter(int flags, ClassLoader classLoader) {
      super(flags);
      this.classLoader = classLoader;
    }

    protected String getCommonSuperClass(String type1, String type2) {
      try {
        ClassReader info1 = typeInfo(type1);
        ClassReader info2 = typeInfo(type2);
        String 
        superType = checkImplementInterface(type1, type2, info1, info2);
        if (superType != null) return superType;
        superType = checkImplementInterface(type2, type1, info2, info1);
        if (superType != null) return superType;

        StringBuilder b1 = typeAncestors(type1, info1);
        StringBuilder b2 = typeAncestors(type2, info2);
        String result = JAVA_LANG_OBJECT;
        int end1 = b1.length();
        int end2 = b2.length();
        while (true) {
          int start1 = b1.lastIndexOf(";", end1 - 1);
          int start2 = b2.lastIndexOf(";", end2 - 1);
          if (start1 != -1 && start2 != -1 && end1 - start1 == end2 - start2) {
            String p1 = b1.substring(start1 + 1, end1);
            String p2 = b2.substring(start2 + 1, end2);
            if (p1.equals(p2)) {
              result = p1;
              end1 = start1;
              end2 = start2;
            } else {
              return result;
            }
          } else {
            return result;
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e.toString());
      }
    }

    private String checkImplementInterface(String type1, String type2, ClassReader info1, ClassReader info2) throws IOException {
      if ((info1.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
        if (typeImplements(type2, info2, type1)) {
          return type1;
        }
        return JAVA_LANG_OBJECT;
      }
      return null;
    }

    private StringBuilder typeAncestors(String type, ClassReader info) throws IOException {
      StringBuilder b = new StringBuilder();
      while (!JAVA_LANG_OBJECT.equals(type)) {
        b.append(';').append(type);
        type = info.getSuperName();
        info = typeInfo(type);
      }
      return b;
    }

    private boolean typeImplements(String type, ClassReader classReader, String interfaceName) throws IOException {
      while (!JAVA_LANG_OBJECT.equals(type)) {
        String[] itfs = classReader.getInterfaces();
        for (int i = 0; i < itfs.length; ++i) {
          if (itfs[i].equals(interfaceName)) {
            return true;
          }
        }
        for (int i = 0; i < itfs.length; ++i) {
          if (typeImplements(itfs[i], typeInfo(itfs[i]), interfaceName)) {
            return true;
          }
        }
        type = classReader.getSuperName();
        classReader = typeInfo(type);
      }
      return false;
    }

    private ClassReader typeInfo(final String type) throws IOException {
      InputStream is = null;
      try {
        is = classLoader.getResourceAsStream(type + ".class");
        return new ClassReader(is);
      }
      finally {
        if (is != null) {
          is.close();
        }
      }
    }
  }
}
