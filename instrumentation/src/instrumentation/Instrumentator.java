package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.util.classFinder.ClassFinder;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.util.ClassNameUtil;
import com.intellij.rt.coverage.util.ErrorReporter;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;


public class Instrumentator {

  public static void premain(String argsString, Instrumentation instrumentation) throws Exception {
    String[] args = tokenize(argsString);
    if (args.length == 1) {
      try {
        args = readArgsFromFile(args[0]);
      } catch (IOException e) {
        ErrorReporter.reportError("Arguments were not passed correctly", e);
        return;
      }
    }
    final boolean traceLines = Boolean.valueOf(args[1]).booleanValue();
    final boolean sampling = Boolean.valueOf(args[4]).booleanValue();
    final File dataFile = new File(args[0]);
    final boolean calcUnloaded = Boolean.valueOf(args[2]).booleanValue();
    ProjectData initialData = null;
    if (Boolean.valueOf(args[3]).booleanValue() && dataFile.isFile()) {
      initialData = ProjectDataLoader.load(dataFile);
    }
    final ProjectData data = ProjectData.createProjectData(dataFile, initialData, traceLines, sampling);
    final List includePatterns = new ArrayList();
    System.out.println("---- IntelliJ IDEA coverage runner ---- ");
    System.out.println(sampling ? "sampling ..." : ("tracing " + (traceLines ? "and tracking per test coverage ..." : "...")));
    final String excludes = "-exclude";
    int i = 5;
    System.out.println("include patterns:");
    for (; i < args.length; i++) {
      if (excludes.equals(args[i])) break;
      includePatterns.add(Pattern.compile(args[i]));
      System.out.println(args[i]);
    }
    System.out.println("exclude patterns:");
    i++;
    final List excludePatterns = new ArrayList();
    for (; i < args.length; i++) {
      excludePatterns.add(Pattern.compile(args[i]));
      System.out.println(args[i]);
    }

    final ClassFinder cf = new ClassFinder(includePatterns, excludePatterns);
    Runtime.getRuntime().addShutdownHook(new Thread(new SaveHook(dataFile, calcUnloaded, cf)));

    instrumentation.addTransformer(new ClassFileTransformer() {
      public byte[] transform(ClassLoader loader,
                              String className,
                              Class classBeingRedefined,
                              ProtectionDomain protectionDomain,
                              byte[] classfileBuffer) throws IllegalClassFormatException {
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
          if (className.startsWith("com.intellij.rt")
           || className.startsWith("java.")
           || className.startsWith("sun.")
           || className.startsWith("gnu.trove")
           || className.startsWith("org.objectweb.asm")
           || className.startsWith("org.apache.oro.text.regex")) {
            return null;
          }

          // apply include and exclude patterns to parent class name only
          className = ClassNameUtil.getOuterClassName(className);
          for (Iterator it = excludePatterns.iterator(); it.hasNext();) {
            if (((Pattern)it.next()).matcher((className)).matches()) return null;
          }

          cf.addClassLoader(loader);
          if (includePatterns.isEmpty() && loader != null) {
            return instrument(classfileBuffer, data);
          }
          for (Iterator it = includePatterns.iterator(); it.hasNext();) {
            if (((Pattern)it.next()).matcher(className).matches()) {
              return instrument(classfileBuffer, data);
            }
          }
        }
        catch (Throwable e) {
          ErrorReporter.reportError("Error during class instrumentation: " + className, e);
        }
        return null;
      }
    });
  }

  private static String[] readArgsFromFile(String arg) throws IOException {
    final List result = new ArrayList();
    final File file = new File(arg);
    final BufferedReader reader = new BufferedReader(new FileReader(file));
    try {
      while(reader.ready()) {
        result.add(reader.readLine());
      }
    } finally {
      reader.close();
    }
    return (String[]) result.toArray(new String[result.size()]);
  }

  private static byte[] instrument(final byte[] classfileBuffer, final ProjectData data) {
    final ClassReader cr = new ClassReader(classfileBuffer);
    final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    final ClassAdapter cv = data.isSampling() ? (ClassAdapter)new SamplingInstrumenter(data, cw) : new ClassInstrumenter(data, cw);
    cr.accept(cv, 0);
    return cw.toByteArray();
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
}
